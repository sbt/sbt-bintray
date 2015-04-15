package bintray

import sbt._
import bintry.Licenses
import scala.util.Try

object Bintray {
  val defaultMavenRepository = "maven"
  // http://www.scala-sbt.org/0.13/docs/Bintray-For-Plugins.html
  val defaultSbtPluginRepository = "sbt-plugins"

  def whoami(credsFile: File, log: Logger): String =
    {
      val is = BintrayCredentials.read(credsFile)
        .fold(sys.error(_), _ match {
          case None =>
            "nobody"
          case Some(BintrayCredentials(user, _)) =>
            user
        })
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
          s"One or more of the defined licenses where not among the following allowed licenses\n $acceptable")
      }
    }

  def withRepo[A](credsFile: File, org: Option[String], repoName: String, prompt: Boolean = true)
    (f: BintrayRepo => A): Option[A] =
    ensuredCredentials(credsFile, prompt) map { cred =>
      f(BintrayRepo(cred, org, repoName))
    }

  private[bintray] def ensuredCredentials(
    credsFile: File, prompt: Boolean = true): Option[BintrayCredentials] =
    BintrayCredentials.read(credsFile).fold(sys.error(_), _ match {
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
    BintrayCredentials.read(credsFile).fold(sys.error(_), _ match {
      case None =>
        saveBintrayCredentials(credsFile)(requestCredentials())
      case Some(BintrayCredentials(user, pass)) =>
        saveBintrayCredentials(credsFile)(requestCredentials(Some(user), Some(pass)))
    })    

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
    val name = Prompt("Enter bintray username%s" format(
      defaultName.map(" (%s)".format(_)).getOrElse(""))).orElse(defaultName).getOrElse {
      sys.error("bintray username required")
    }
    val pass = Prompt.descretely("Enter bintray API key %s" format(
      defaultKey.map(_ => "(use current)").getOrElse("(under https://bintray.com/profile/edit)")))
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
        sbt.Process("git" :: "remote" :: "-v" :: Nil).!!.split("\n")
         .map {
           _.split("""\s+""") match {
             case Array(name, url, "(push)") =>
               Some((name, url))
             case e =>
               None
           }
         }.flatten
      pushes
        .find { case (name, _) => "origin" == name }
        .orElse(pushes.headOption)
        .map { case (_, url) => url }
    }  
}
