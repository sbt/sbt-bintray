package bintray

import sbt._
import bintry.{ Licenses, Client }
import scala.util.Try
import scala.collection.concurrent.TrieMap

object Bintray {
  val defaultMavenRepository = "maven"
  // http://www.scala-sbt.org/0.13/docs/Bintray-For-Plugins.html
  val defaultSbtPluginRepository = "sbt-plugins"

  def publishTo(repo: Client#Repo, pkg: Client#Repo#Package, version: String,
    mvnStyle: Boolean = true, isSbtPlugin: Boolean = false, release: Boolean = false): Resolver =
    RawRepository(
      (mvnStyle, isSbtPlugin) match {
        case (true, true) =>
          BintrayMavenSbtPluginResolver(
            s"Bintray-Sbt-Maven-Publish-${repo.subject}-${repo.repo}-${pkg.name}",
            pkg.version(version), release)
        case (true, _) =>
          BintrayMavenResolver(
            s"Bintray-Maven-Publish-${repo.subject}-${repo.repo}-${pkg.name}",
            s"https://api.bintray.com/maven/${repo.subject}/${repo.repo}/${pkg.name}", pkg, release, ignoreExists = false)
        case (false, _) =>
          BintrayIvyResolver(
            s"Bintray-${if (isSbtPlugin) "Sbt" else "Ivy"}-Publish-${repo.subject}-${repo.repo}-${pkg.name}",
            pkg.version(version), release)
      })

  def remoteCache(repo: Client#Repo, pkg: Client#Repo#Package): Resolver =
    RawRepository(
      BintrayMavenResolver(
        s"Bintray-Remote-Cache-${repo.subject}-${repo.repo}-${pkg.name}",
        s"https://api.bintray.com/maven/${repo.subject}/${repo.repo}/${pkg.name}", pkg, true, true)
    )

  def whoami(creds: Option[BintrayCredentials], log: Logger): String =
    {
      val is = creds match {
        case None => "nobody"
        case Some(BintrayCredentials(user, _)) => user
      }
      log.info(is)
      is
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

  def withRepo[A](context: BintrayCredentialContext, org: Option[String], repoName: String, log: Logger)
    (f: BintrayRepo => A): Option[A] =
    ensuredCredentials(context, log) map { cred =>
      val repo = cachedRepo(cred, org, repoName)
      f(repo)
    }

  private val repoCache: TrieMap[(BintrayCredentials, Option[String], String), BintrayRepo] = TrieMap()
  def cachedRepo(credential: BintrayCredentials, org: Option[String], repoName: String): BintrayRepo =
    repoCache.synchronized {
      // lock to avoid creating and leaking HTTP client threadpools
      // see: https://github.com/sbt/sbt-bintray/issues/144
      repoCache.getOrElseUpdate((credential, org, repoName), BintrayRepo(credential, org, repoName))
    }

  private[bintray] def ensuredCredentials(
    context: BintrayCredentialContext, log: Logger): Option[BintrayCredentials] =
      propsCredentials(context)
        .orElse(envCredentials(context))
        .orElse(BintrayCredentials.read(context.credsFile))

  private def propsCredentials(context: BintrayCredentialContext) =
    for {
      name <- sys.props.get(context.userNameProp)
      pass <- sys.props.get(context.passProp)
    } yield BintrayCredentials(name, pass)

  private def envCredentials(context: BintrayCredentialContext) =
    for {
      name <- sys.env.get(context.userNameEnv)
      pass <- sys.env.get(context.passEnv)
    } yield BintrayCredentials(name, pass)

  /** assign credentials or ask for new ones */
  private[bintray] def changeCredentials(context: BintrayCredentialContext, log: Logger): Unit =
    Bintray.ensuredCredentials(context, Logger.Null) match {
      case None =>
        saveBintrayCredentials(context.credsFile)(requestCredentials(), log)
      case Some(BintrayCredentials(user, pass)) =>
        saveBintrayCredentials(context.credsFile)(requestCredentials(Some(user), Some(pass)), log)
    }

  private[bintray] def buildResolvers(creds: Option[BintrayCredentials], org: Option[String], repoName: String, mavenStyle: Boolean): Seq[Resolver] =
    creds.map {
      case BintrayCredentials(user, _) => Seq(
        if (mavenStyle) Resolver.bintrayRepo(org.getOrElse(user), repoName)
        else Resolver.bintrayIvyRepo(org.getOrElse(user), repoName)
      )
    } getOrElse Nil

  private def saveBintrayCredentials(to: File)(creds: (String, String), log: Logger) = {
    log.info(s"saving credentials to $to")
    val (name, pass) = creds
    BintrayCredentials.writeBintray(name, pass, to)
    log.info("reload project for sbt setting `publishTo` to take effect")
  }

  // todo: generalize this for both bintray & sonatype credential prompts
  private def requestCredentials(
    defaultName: Option[String] = None,
    defaultKey: Option[String] = None): (String, String) = {
    val name = Prompt("Enter bintray username%s" format defaultName.map(" (%s)".format(_)).getOrElse("")).orElse(defaultName).getOrElse {
      sys.error("bintray username required")
    }
    val pass = Prompt.descretely("Enter bintray API key %s" format defaultKey.map(_ => "(use current)").getOrElse("(under https://bintray.com/profile/edit)"))
        .orElse(defaultKey).getOrElse {
          sys.error("bintray API key required")
        }
    (name, pass)
  }

  private[bintray] object await {
    import scala.concurrent.{ Await, Future }
    import scala.concurrent.duration.Duration

    def result[T](f: => Future[T]) = Await.result(f, Duration.Inf)
    def ready[T](f: => Future[T]) = Await.ready(f, Duration.Inf)
  }

  def resolveVcsUrl: Try[Option[String]] =
    Try {
      val pushes =
        sys.process.Process("git" :: "remote" :: "-v" :: Nil).!!.split("\n")
         .flatMap {
           _.split("""\s+""") match {
             case Array(name, url, "(push)") => Some((name, url))
             case _                          => None
           }
         }

      pushes
        .find { case (name, _) => "origin" == name }
        .orElse(pushes.headOption)
        .map { case (_, url) => url }
    }
}
