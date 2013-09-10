package bintray

import java.io.File
import sbt._
import bintry._
import dispatch._, dispatch.Defaults

object Plugin extends sbt.Plugin {
  import sbt.Keys._
  import bintray.Keys._

  // This add the version attributes after publishing.
  private def publishWithVersionAttrs: Def.Initialize[Task[Unit]] =
    (publish, publishVersionAttributes) apply { (p, attr) =>
      // Try to publish first, then publish version attributes.
      p && attr
    }

  private def publishVersionAttributesTask: Def.Initialize[Task[Unit]] =
    (ensureCredentials, bintrayOrganization in bintray, repository in bintray, versionAttributes in bintray, name, version).map {
      (ensureCredentials, btyOrg, repository, versionAttributes, name, version) =>
        val tmp = ensureCredentials
        val BintrayCredentials(user, key) = tmp
        val bty = Client(user, key).repo(btyOrg.getOrElse(user), repository)
        val attributes = versionAttributes.toList
        bty.get(name).version(version).attrs.update(attributes:_*)(Noop)()
        ()
    }

  /** Ensure user-specific bintray package exists */
  private def ensurePackageTask: Def.Initialize[Task[Unit]] =
    (ensureCredentials,
     bintrayOrganization in bintray,
     repository in bintray,
     name,
     description in bintray,
     packageLabels in bintray,
     packageAttributes in bintray,
     licenses,
     streams).map {
      case (BintrayCredentials(user, key), btyOrg, repo, name, desc, labels, attrs, licenses, out) =>
            val bty = Client(user, key).repo(btyOrg.getOrElse(user), repo)
            val exists =
              if (bty.get(name)(new FunctionHandler(_.getStatusCode != 404))()) {
                // update existing attrs
                if (!attrs.isEmpty) bty.get(name).attrs.update(attrs.toList:_*)(Noop)()
                true
              }
              else {
                val created = bty.createPackage(name, desc, licenses.map(_._1), labels:_*)(
                  new FunctionHandler(_.getStatusCode == 201))()
                  // assign attrs
                  if (created && !attrs.isEmpty) bty.get(name).attrs.set(attrs.toList:_*)(Noop)()
                  created
              }
              if (!exists) sys.error("was not able to find or create a package for %s in repo %s named %s"
                                   .format(btyOrg.getOrElse(user), repo, name))
    }

  /** set a user-specific publishTo endpoint */
  private def publishToBintray: Def.Initialize[Option[Resolver]] =
    (credentialsFile in bintray,
     bintrayOrganization in bintray,
     repository in bintray,
     name,
     streams,
     version,
     sbtPlugin).apply {
      case (creds, btyOrg, repo, pkg, out, version, isSbtPlugin) =>
        ensuredCredentials(creds, prompt = false).map {
          case BintrayCredentials(user, pass) =>
            val client = Client(user, pass)
            val cr = client.repo(btyOrg.getOrElse(user), repo)
            val cp = cr.get(pkg)
            Opts.resolver.publishTo(cr, cp, version, isSbtPlugin)
        }
    }

  private def unpublishTask: Def.Initialize[Task[Unit]] =
    (ensureCredentials, bintrayOrganization in bintray, repository in bintray, name, version, streams).map {
        (ensureCredentials, btyOrg, repository, name, version, streams) =>
      val tmp = ensureCredentials
      val BintrayCredentials(user, key) = tmp
      val bty = Client(user, key).repo(btyOrg.getOrElse(user), repository)
      val pkg = name
      val vers = version
      val log = streams.log
      val (status, body) = bty.get(pkg).version(vers).delete(new FunctionHandler({ r => (r.getStatusCode, r.getResponseBody)}))()
      if (status == 200) log.info("%s@%s was discarded" format(pkg, vers))
      else sys.error("failed to discard %s%s: %s" format(pkg, vers, body))
    }

  /** if credentials exist, append a user-specific resolver */
  private def appendBintrayResolver: Def.Initialize[Seq[Resolver]] =
    (credentialsFile in bintray,
     bintrayOrganization in bintray,
     repository in bintray).apply {
      (creds, btyOrg, repo) =>
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
    (streams,
     credentialsFile in bintray,
     repository in bintray, name).map {
      (out, creds, repo, name) =>
        ensuredCredentials(creds, prompt = true).map {
          case BintrayCredentials(user, pass) =>
            import org.json4s._
            import JsonImplicits._
            val pkg = Client(user, pass).repo(user, repo).get(name)
            out.log.info("fetching package versions for package %s" format name)
            pkg(EitherHttp({ _ => JNothing}, as.json4s.Json))().fold({ js =>
              out.log.warn("package does not exist")
              Nil
            }, { js =>
              for {
                JObject(fs) <- js
                ("versions", JArray(versions)) <- fs
                JString(versionString) <- versions
              } yield {
                out.log.info("- %s" format versionString)
                versionString
              }
            })
        }.getOrElse(Nil)
    }

  /** assign credentials or ask for new ones */
  private def changeCredentialsTask: Def.Initialize[Task[Unit]] =
    (streams, credentialsFile in bintray).map {
      (out, creds) =>
        BintrayCredentials.read(creds).fold(sys.error(_), _ match {
          case None =>
            saveCredentials(creds)(requestCredentials())
          case Some(BintrayCredentials(user, pass)) =>
            saveCredentials(creds)(requestCredentials(Some(user), Some(pass)))
        })
    }

  private def whoamiTask: Def.Initialize[Task[String]] =
    (streams, credentialsFile in bintray).map {
      case (out, creds) =>
        BintrayCredentials.read(creds).fold(sys.error(_), _ match {
          case None =>
            out.log.info("nobody")
            "nobody"
          case Some(BintrayCredentials(user, _)) =>
            out.log.info(user)
            user
        })
    }

  private def ensureCredentialsTask: Def.Initialize[Task[BintrayCredentials]] =
    (streams, credentialsFile in bintray) map {
      (out, creds) => ensuredCredentials(creds).get
    }

  private def ensureLicensesTask: Def.Initialize[Task[Unit]] =
    (licenses) map {
      (ls) =>
        if (ls.isEmpty) sys.error("you must define at least one license for this project. Please choose one or more of %s"
                                  .format(Licenses.Names.mkString(",")))
        if (!ls.forall { case (name, _) => Licenses.Names.contains(name) }) sys.error(
          "One or more of the defined licenses where not amoung the following allowed liceses %s"
          .format(Licenses.Names.mkString(",")))
    }

  private def requestCredentials(
    defaultName: Option[String] = None,
    defaultKey: Option[String] = None) = {
    val name = Prompt("Enter bintray username%s" format(
      defaultName.map(" (%s)".format(_)).getOrElse(""))).orElse(defaultName)
    if (!name.isDefined) sys.error("bintray username required")
    val pass = Prompt.descretely("Enter bintray API key %s" format(
      defaultKey.map(_ => "(use current)").getOrElse("(under https://bintray.com/profile/edit)"))).orElse(defaultKey)
    if (!pass.isDefined) sys.error("bintray API key required")
    (name.get, pass.get)
  }

  private def saveCredentials(to: File)(creds: (String, String)) = {
    println("saving credentials to %s" format to)
    val (name, pass) = creds
    BintrayCredentials.write(name, pass, to)
    println("reload project for publishTo to take effect")
  }

  private def ensuredCredentials(creds: File, prompt: Boolean = true): Option[BintrayCredentials] =
    BintrayCredentials.read(creds).fold(sys.error(_), _ match {
      case None =>
        if (prompt) {
          println("bintray-sbt requires your bintray credentials.")
          saveCredentials(creds)(requestCredentials())
          ensuredCredentials(creds, prompt)
        } else {
          println("Missing bintray credentials %s. Some bintray features depend on this." format creds)
          None
        }
      case creds => creds
    })

  def bintrayPublishSettings: Seq[Setting[_]] = seq(
    credentialsFile in bintray in Global := Path.userHome / ".bintray" / ".credentials",
    bintrayOrganization in bintray in Global <<= sbtPlugin { sbtPlugin => if (sbtPlugin) Some("sbt") else None },
    repository in bintray in Global <<= sbtPlugin { sbtPlugin => if (sbtPlugin) "sbt-plugin-releases" else "maven" },
    publishMavenStyle <<= (sbtPlugin, publishMavenStyle) { (sbtPlugin, publishMavenStyle) =>
      if (sbtPlugin) false else publishMavenStyle
    },
    packageLabels in bintray := Nil,
    description in bintray <<= description,
    publishTo in bintray <<= publishToBintray,
    resolvers in bintray <<= appendBintrayResolver,
    credentials in bintray <<= (credentialsFile in bintray).map(Credentials(_) :: Nil),
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
    unpublish in bintray <<= unpublishTask.dependsOn(ensureBintrayPackageExists, ensureLicenses)
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
