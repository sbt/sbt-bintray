package bintray

import bintry.{Client, Licenses}
import sbt._

import scala.collection.concurrent.TrieMap
import scala.util.Try

object Bintray {
  val defaultMavenRepository = "maven"
  // http://www.scala-sbt.org/0.13/docs/Bintray-For-Plugins.html
  val defaultSbtPluginRepository = "sbt-plugins"

  def publishTo(repo: Client#Repo, pkg: Client#Repo#Package, version: String,
    mvnStyle: Boolean = true, isSbtPlugin: Boolean = false, release: Boolean = false): Resolver =
    if (mvnStyle) new RawRepository(
      BintrayMavenResolver(s"Bintray-Maven-Publish-${repo.subject}-${repo.repo}-${pkg.name}",
                           s"https://api.bintray.com/maven/${repo.subject}/${repo.repo}/${repo.repo}", pkg, release))
    else new RawRepository(
      BintrayIvyResolver(s"Bintray-${if (isSbtPlugin) "Sbt" else "Ivy"}-Publish-${repo.subject}-${repo.repo}-${pkg.name}",
                         pkg.version(version),
                         sbt.Resolver.ivyStylePatterns.artifactPatterns, release))

  def whoami(creds: Option[BintrayCredentials], log: Logger): String =
    {
      val is = creds match {
          case None =>
            "nobody"
          case Some(BintrayCredentials(user, _)) =>
            user
      }
      log.info(is)
      is
    }

  private[bintray] def loadedCredentials(settingCreds: Option[BintrayCredentials], credsFile: File, log: Logger) = {
    def fileCreds = BintrayCredentials.read(credsFile).fold(
      err => {
        log.warn(err)
        None
      }, {
        case None =>
          log.warn("No bintray credentials found.")
          None
        case creds => creds
      }
    )

    settingCreds.orElse(fileCreds)
  }

  private[bintray] def ensureLicenses(licenses: Seq[(String, URL)], omit: Boolean): Unit =
    {
      val acceptable = Licenses.Names.toSeq.sorted.mkString(", ")
      if (!omit) {
        if (licenses.isEmpty) sys.error(
          s"you must define at least one license for this project. Please choose one or more of\n $acceptable")
        if (!licenses.forall { case (name, _) => Licenses.Names.contains(name) }) sys.error(
          s"One or more of the defined licenses were not among the following allowed licenses\n $acceptable")
      }
    }

  def withRepo[A](credOption: Option[BintrayCredentials], org: Option[String], repoName: String)
    (f: BintrayRepo => A): Option[A] = credOption.map { cred =>
      val repo = cachedRepo(cred, org, repoName)
      f(repo)
    }

  private val repoCache: TrieMap[(BintrayCredentials, Option[String], String), BintrayRepo] = TrieMap()
  def cachedRepo(credential: BintrayCredentials, org: Option[String], repoName: String): BintrayRepo =
    repoCache.getOrElseUpdate((credential, org, repoName), BintrayRepo(credential, org, repoName))

  private[bintray] def ensuredCredentials(
    credsFile: File, prompt: Boolean = true): Option[BintrayCredentials] =
    BintrayCredentials.read(credsFile).fold(sys.error, {
      case None =>
        if (prompt) {
          println("bintray-sbt requires your bintray credentials.")
          saveBintrayCredentials(credsFile)(requestCredentials())
          ensuredCredentials(credsFile, prompt)
        } else {
          println(s"Missing bintray credentials $credsFile. Some bintray features depend on this.")
          None
        }
      case creds => creds
    })

  /** assign credentials or ask for new ones */
  private[bintray] def changeCredentials(credsFile: File): Unit =
    BintrayCredentials.read(credsFile).fold(sys.error, {
      case None =>
        saveBintrayCredentials(credsFile)(requestCredentials())
      case Some(BintrayCredentials(user, pass)) =>
        saveBintrayCredentials(credsFile)(requestCredentials(Some(user), Some(pass)))
    })    

  private[bintray] def buildResolvers(credsOpt: Option[BintrayCredentials], org: Option[String], repoName: String): Seq[Resolver] =
    credsOpt.map { creds =>
      Seq(Resolver.bintrayRepo(org.getOrElse(creds.user), repoName))
    }.getOrElse(Seq.empty)

  private def saveBintrayCredentials(to: File)(creds: (String, String)) = {
    println(s"saving credentials to $to")
    val (name, pass) = creds
    BintrayCredentials.writeBintray(name, pass, to)
    println("reload project for sbt setting `publishTo` to take effect")
  }

  // todo: generalize this for both bintray & sonatype credential prompts
  private def requestCredentials(
    defaultName: Option[String] = None,
    defaultKey: Option[String] = None): (String, String) = {

    val nameFormat = defaultName.map(" (%s)".format(_)).getOrElse("")
    val name = Prompt("Enter bintray username%s" format nameFormat).orElse(defaultName).getOrElse {
      sys.error("bintray username required")
    }
    val passFormat = defaultKey.map(_ => "(use current)").getOrElse("(under https://bintray.com/profile/edit)")
    val pass = Prompt.descretely("Enter bintray API key %s" format passFormat)
        .orElse(defaultKey).getOrElse {
          sys.error("bintray API key required")
        }
    (name, pass)
  }

  private[bintray] object await {
    import scala.concurrent.duration.Duration
    import scala.concurrent.{Await, Future}

    def result[T](f: => Future[T]) = Await.result(f, Duration.Inf)
    def ready[T](f: => Future[T]) = Await.ready(f, Duration.Inf)
  }

  def resolveVcsUrl: Try[Option[String]] =
    Try {
      val pushes =
        sys.process.Process("git" :: "remote" :: "-v" :: Nil).!!.split("\n").flatMap(
          _.split("""\s+""") match {
            case Array(name, url, "(push)") =>
              Some((name, url))
            case _ => None
          }
        )
      pushes
        .find { case (name, _) => "origin" == name }
        .orElse(pushes.headOption)
        .map { case (_, url) => url }
    }  
}
