package bintray

import java.io.File
import sbt._
import bintry._
import dispatch._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{ Await, Future }
import scala.concurrent.duration.Duration

// http://www.scala-sbt.org/0.13.0/docs/Community/ChangeSummary_0.13.0.html#control-over-automatically-added-settings
object Plugin extends sbt.Plugin with DispatchHandlers {
  import sbt.Keys._
  import bintray.Keys._
  private object PasswordCache {
    private val underlying = new java.util.concurrent.ConcurrentHashMap[String, String]()
    def pgp = Option(underlying.get("pgp"))
  }

  object await {
    def result[T](f: => Future[T]) = Await.result(f, Duration.Inf)
    def ready[T](f: => Future[T]) = Await.ready(f, Duration.Inf)
  }

  // publishes version attributes after publishing artifact files
  private def publishWithVersionAttrs: Def.Initialize[Task[Unit]] =
    (publish, publishVersionAttributes)(_ && _)

  private def publishVersionAttributesTask: Def.Initialize[Task[Unit]] =
    Def.task {
      val tmp = ensureCredentials.value
      val btyOrg = (bintrayOrganization in bintray).value
      val repo = (repository in bintray).value
      val versionAttrs = (versionAttributes in bintray).value
      val pkg = (name in bintray).value
      val vers = version.value
      val BintrayCredentials(user, key) = tmp
      val bty = Client(user, key).repo(btyOrg.getOrElse(user), repo)
      val attributes = versionAttrs.toList
      await.ready(bty.get(pkg).version(vers).attrs.update(attributes:_*)(Noop))
    }

  /** Ensure user-specific bintray package exists */
  private def ensurePackageTask: Def.Initialize[Task[Unit]] =
    Def.task {
      val tmp = ensureCredentials.value
      val BintrayCredentials(user, key) = tmp
      val btyOrg = (bintrayOrganization in bintray).value
      val repo = (repository in bintray).value
      val pkg = (name in bintray).value
      val desc = (description in bintray).value
      val labels = (packageLabels in bintray).value
      val attrs = (packageAttributes in bintray).value
      val lics = licenses.value
      val bty = Client(user, key).repo(btyOrg.getOrElse(user), repo)
      val exists =
        if (await.result(bty.get(pkg)(asFound))) {
          // update existing attrs
          if (!attrs.isEmpty) await.ready(bty.get(pkg).attrs.update(attrs.toList:_*)(Noop))
          true
        } else {
          val created = await.result(
            bty.createPackage(pkg)
              .desc(desc)
              .licenses(lics.map(_._1):_*)
              .labels(labels:_*)(asCreated))
          // assign attrs
          if (created && !attrs.isEmpty) await.ready(
            bty.get(pkg).attrs.set(attrs.toList:_*)(Noop))
          created
        }
      if (!exists) sys.error(
        s"was not able to find or create a package for ${btyOrg.getOrElse(user)} in repo $repo named $name")
    }

  private def defaultBintrayCredentials: Def.Initialize[Option[BintrayCredentials]] =
    Def.setting {
      ensuredCredentials((credentialsFile in bintray).value, Nil)
    }

  /** set a user-specific bintray publishTo endpoint */
  private def publishToBintray: Def.Initialize[Option[Resolver]] =
    Def.setting {
      val credsFile = (credentialsFile in bintray).value
      val creds = (bintrayCredentials in bintray).value
      val btyOrg = (bintrayOrganization in bintray).value
      val repo = (repository in bintray).value
      val pkg = (name in bintray).value
      val vers = version.value
      val mvnStyle = publishMavenStyle.value
      val isSbtPlugin = sbtPlugin.value
      ensuredCredentials(credsFile, creds.map(BintrayCredentials.api.toDirect(_)).toSeq, prompt = false).map {
        case BintrayCredentials(user, pass) =>
          val cr = Client(user, pass).repo(btyOrg.getOrElse(user), repo)
          val cp = cr.get(pkg)
          if ("maven" == repo && !mvnStyle) println(
            "you have opted to publish to a repository named 'maven' but publishMavenStyle is assigned to false. This may result in unexpected behavior")
          Opts.resolver.publishTo(cr, cp, vers, mvnStyle, isSbtPlugin)
      }
    }

  private def unpublishTask: Def.Initialize[Task[Unit]] =
    Def.task {
      val tmp = ensureCredentials.value
      val BintrayCredentials(user, key) = tmp
      val btyOrg = (bintrayOrganization in bintray).value
      val repo = (repository in bintray).value
      val bty = Client(user, key).repo(btyOrg.getOrElse(user), repo)
      val pkg = (name in bintray).value
      val vers = version.value
      val log = streams.value.log
      val (status, body) = await.result(
        bty.get(pkg).version(vers).delete(asStatusAndBody))
      if (status == 200) log.info(s"$pkg@$vers was discarded")
      else sys.error(s"failed to discard $pkg@$vers: $body")
    }

  private def remoteSignTask: Def.Initialize[Task[Unit]] =
    Def.task {
      val log = streams.value.log
      val tmp = ensureCredentials.value
      val BintrayCredentials(user, key) = tmp
      val btyOrg = (bintrayOrganization in bintray).value
      val repo = (repository in bintray).value
      val bty = Client(user, key).repo(btyOrg.getOrElse(user), repo)
      val pkg = (name in bintray).value
      val vers = version.value
      val btyVersion = bty.get(pkg).version(vers)
      val passphrase = Prompt.descretely("Enter pgp passphrase").getOrElse {
        sys.error("pgp passphrase is required")
      }
      val (status, body) = await.result(
        btyVersion.sign(passphrase)(asStatusAndBody))
      if (status == 200) {
        log.info(s"$pkg@$vers was signed")
        // after signing the remote artifacts, they remain
        // unpublished (not available for download)
        // we are opting to publish those unpublished
        // artifacts here
        val (pubStatus, pubBody) = await.result(
          btyVersion.publish(asStatusAndBody))
        if (pubStatus != 200) {
          sys.error(s"failed to publish signed artifacts: $pubBody")
        }
      }
      else sys.error(s"failed to sign $pkg@$vers: $body")
    }

  private def syncMavenCentralTask: Def.Initialize[Task[Unit]] =
    Def.task {
      val log = streams.value.log
      val tmp = ensureCredentials.value
      val BintrayCredentials(user, key) = tmp
      val repo = (repository in bintray).value
      val btyOrg = (bintrayOrganization in bintray).value
      val bty = Client(user, key).repo(btyOrg.getOrElse(user), repo)
      val pkg = (name in bintray).value
      val vers = version.value
      val creds = credentials.value
      val btyVersion = bty.get(pkg).version(vers)
      val BintrayCredentials(sonauser, sonapass) =
        resolveSonatypeCredentials(creds)
      val (status, body) = await.result(
        btyVersion.mavenCentralSync(sonauser, sonapass)(asStatusAndBody))
      status match {
        case ok if ok == 200 => log.info(s"$pkg@$vers was synced with maven central")
        case nf if nf == 404 => log.info(s"$pkg@$vers was not found. try creating this version by typing `publish`")
        case _ => sys.error(s"failed to sync $pkg@$vers with maven central: $body")
      }
    }

  /** if credentials exist, append a user-specific resolver */
  private def appendBintrayResolver: Def.Initialize[Seq[Resolver]] =
    Def.setting {
      val creds = (credentialsFile in bintray).value
      val btyOrg = (bintrayOrganization in bintray).value
      val repo = (repository in bintray).value
      BintrayCredentials.read(creds).fold({ err =>
        println("bintray credentials %s is malformed".format(err))
        Nil
      }, {
        _.map { case BintrayCredentials(user, _) => Seq(Opts.resolver.repo(btyOrg.getOrElse(user), repo)) }
         .getOrElse(Nil)
      })
    }

  /** Lists versions of bintray packages corresponding to the current project */
  private def packageVersionsTask: Def.Initialize[Task[Seq[String]]] =
    Def.task {
      val log = streams.value.log
      val credsFile = (credentialsFile in bintray).value
      val creds = (bintrayCredentials in bintray).value      
      val repo = (repository in bintray).value
      val pkgName = (name in bintray).value
      ensuredCredentials(credsFile, creds.map(BintrayCredentials.api.toDirect).toSeq, prompt = true).map {
        case BintrayCredentials(user, pass) =>
          import org.json4s._
          val pkg = Client(user, pass).repo(user, repo).get(pkgName)
          log.info(s"fetching package versions for package $pkgName")
          await.result(pkg(EitherHttp({ _ => JNothing}, as.json4s.Json))).fold({ js =>
            log.warn("package does not exist")
            Nil
          }, { js =>
            for {
              JObject(fs) <- js
              ("versions", JArray(versions)) <- fs
              JString(versionString) <- versions
            } yield {
              log.info(s"- $versionString")
              versionString
            }
          })
      }.getOrElse(Nil)
    }

  /** assign credentials or ask for new ones */
  private def changeCredentialsTask: Def.Initialize[Task[Unit]] =
    (credentialsFile in bintray).map {
      credsFile =>
        BintrayCredentials.read(credsFile).fold(sys.error(_), _ match {
          case None =>
            saveBintrayCredentials(credsFile)(requestCredentials())
          case Some(BintrayCredentials(user, pass)) =>
            saveBintrayCredentials(credsFile)(requestCredentials(Some(user), Some(pass)))
        })
    }

  private def whoamiTask: Def.Initialize[Task[String]] =
    Def.task {
      val log = streams.value.log
      val creds = (credentialsFile in bintray).value
      val is = BintrayCredentials.read(creds).fold(sys.error(_), _ match {
        case None =>
          "nobody"
        case Some(BintrayCredentials(user, _)) =>
          user
      })
      log.info(is)
      is
    }

  private def ensureCredentialsTask: Def.Initialize[Task[BintrayCredentials]] =
    Def.task {
      ensuredCredentials(
        (credentialsFile in bintray).value,
        (bintrayCredentials in bintray).value.map(BintrayCredentials.api.toDirect).toSeq ++ credentials.value).get
    }

  private def resolveSonatypeCredentials(
    creds: Seq[sbt.Credentials]): BintrayCredentials =
    Credentials.forHost(creds, BintrayCredentials.sonatype.Host)
      .map { d => (d.userName, d.passwd) }
      .getOrElse(requestSonatypeCredentials) match {
        case (user, pass) => BintrayCredentials(user, pass)
      }

  private def ensureLicensesTask: Def.Initialize[Task[Unit]] =
    Def.task {
      val ls = licenses.value
      if (ls.isEmpty) sys.error(
        "you must define at least one license for this project. Please choose one or more of %s"
        .format(Licenses.Names.mkString(",")))
      if (!ls.forall { case (name, _) => Licenses.Names.contains(name) }) sys.error(
        s"One or more of the defined licenses where not amoung the following allowed liceses ${Licenses.Names.mkString(",")}")
    }

  private def requestSonatypeCredentials: (String, String) = {
    val name = Prompt("Enter sonatype username")
    if (!name.isDefined) sys.error("sonatype username required")
    val pass = Prompt.descretely("Enter sonatype password")
    if (!pass.isDefined) sys.error("sonatype password is required")
    (name.get, pass.get)
  }

  // todo: generalize this for both bintray & sonatype credential prompts
  private def requestCredentials(
    defaultName: Option[String] = None,
    defaultKey: Option[String] = None): (String, String) = {
    val name = Prompt("Enter bintray username%s" format(
      defaultName.map(" (%s)".format(_)).getOrElse(""))).orElse(defaultName)
    if (!name.isDefined) sys.error("bintray username required")
    val pass = Prompt.descretely("Enter bintray API key %s" format(
      defaultKey.map(_ => "(use current)").getOrElse("(under https://bintray.com/profile/edit)"))).orElse(defaultKey)
    if (!pass.isDefined) sys.error("bintray API key required")
    (name.get, pass.get)
  }

  private def saveBintrayCredentials(to: File)(creds: (String, String)) = {
    println(s"saving credentials to $to")
    val (name, pass) = creds
    BintrayCredentials.writeBintray(name, pass, to)
    println("reload project for publishTo to take effect")
  }

  private def ensuredCredentials(
    credsFile: File, creds: Seq[sbt.Credentials], prompt: Boolean = true): Option[BintrayCredentials] =
    Credentials.forHost(creds, BintrayCredentials.api.Host)
      .map(Credentials.toDirect).map { dc =>
        val resolved = BintrayCredentials(dc.userName, dc.passwd)
        if (!credsFile.exists) {
          BintrayCredentials.writeBintray(dc.userName, dc.passwd, credsFile)
        }
        resolved
      }.orElse {
        BintrayCredentials.read(credsFile).fold(sys.error(_), _ match {
          case None =>
            if (prompt) {
              println("bintray-sbt requires your bintray credentials.")
              saveBintrayCredentials(credsFile)(requestCredentials())
              ensuredCredentials(credsFile, creds, prompt)
            } else {
              println(s"Missing bintray credentials $credsFile. Some bintray features depend on this.")
              None
            }
          case creds => creds
        })
      }

  def bintrayPublishSettings: Seq[Setting[_]] = Seq(
    credentialsFile in bintray in Global := Path.userHome / ".bintray" / ".credentials",
    name in bintray := moduleName.value,
    bintrayOrganization in bintray in Global <<= sbtPlugin { sbtPlugin => if (sbtPlugin) Some("sbt") else None },
    repository in bintray in Global <<= sbtPlugin { sbtPlugin => if (sbtPlugin) "sbt-plugin-releases" else "maven" },
    publishMavenStyle <<= (sbtPlugin, publishMavenStyle) { (sbtPlugin, publishMavenStyle) =>
      if (sbtPlugin) false else publishMavenStyle
    },
    packageLabels in bintray := Nil,
    description in bintray <<= description,
    bintrayCredentials in bintray <<= defaultBintrayCredentials,
    // note: publishTo may not have dependencies. therefore, we can not rely well on inline overrides
    // for inline credentials resolution we recommend defining bintrayCredentials before mixing in the defaults
    publishTo in bintray <<= publishToBintray,
    resolvers in bintray <<= appendBintrayResolver,
    credentials in bintray <<= (credentialsFile in bintray, bintrayCredentials in bintray).map {
      Credentials(_) +: _.map(BintrayCredentials.api.toDirect).toSeq
    },
    packageAttributes in bintray <<= (sbtPlugin, sbtVersion) {
      (plugin, sbtVersion) =>
        if (plugin) Map(AttrNames.sbtPlugin -> Seq(BooleanAttr(plugin)))
        else Map.empty
    },
    versionAttributes in bintray <<= (crossScalaVersions, sbtPlugin, sbtVersion) {
      (scalaVersions, plugin, sbtVersion) =>
        val sv = Map(AttrNames.scalas -> scalaVersions.map(VersionAttr(_)))
        if (plugin) sv ++ Map(AttrNames.sbtVersion-> Seq(VersionAttr(sbtVersion))) else sv
    },
    ensureLicenses <<= ensureLicensesTask,
    ensureCredentials <<= ensureCredentialsTask,
    ensureBintrayPackageExists <<= ensurePackageTask,
    publishVersionAttributes <<= publishVersionAttributesTask,
    unpublish in bintray <<= unpublishTask.dependsOn(ensureBintrayPackageExists, ensureLicenses),
    remoteSign in bintray <<= remoteSignTask,
    syncMavenCentral in bintray <<= syncMavenCentralTask
  ) ++ Seq(
    resolvers <++= resolvers in bintray,
    credentials <++= credentials in bintray,
    publishTo <<= publishTo in bintray,
    // We attach this to publish configruation, so that publish-signed in pgp plugin can work.
    publishConfiguration <<= publishConfiguration.dependsOn(ensureBintrayPackageExists, ensureLicenses), 
    publish <<= publishWithVersionAttrs
  )

  def bintrayCommonSettings: Seq[Setting[_]] = Seq(
    changeCredentials in bintray <<= changeCredentialsTask,
    whoami in bintray <<= whoamiTask
  )

  def bintrayQuerySettings: Seq[Setting[_]] = Seq(
    packageVersions in bintray <<= packageVersionsTask
  )

  def bintrayResolverSettings: Seq[Setting[_]] = Seq(
    resolvers += Opts.resolver.jcenter
  )

  def bintraySettings: Seq[Setting[_]] =
    bintrayCommonSettings ++ bintrayResolverSettings ++ bintrayPublishSettings ++ bintrayQuerySettings
}
