package sbtpackages

import sbt.IO
import java.io.File

final case class BintrayCredentials(user: String, password: String) extends RepoCredentials {
  override def toString = s"BintrayCredentials($user, ${"x"*password.size})"
}

final case class GitHubCredentials(user: String, password: String) extends RepoCredentials {
  override def toString = s"GitHubCredentials($user, ${"x"*password.size})"
}

trait RepoCredentials {
  def user: String
  def password: String
}

object RepoCredentials {
  val Keys = Seq("realm", "host", "user", "password")
  def templateSrc(realm: String, host: String)(
    name: String, password: String) =
    s"""realm = $realm
       |host = $host
       |user = $name
       |password = $password""".stripMargin


  /** bintray api */
  object bintray {
    def toDirect(bc: BintrayCredentials) =
      sbt.Credentials(Realm, Host, bc.user, bc.password)
    val Host = "api.bintray.com"
    val Realm = "Bintray API Realm"
    val template = templateSrc(Realm, Host)_
  }

  object github {
    val Host = "api.github.com"
    val Realm = "GitHub API Realm"
    val template = templateSrc(Realm, Host)_
  }

  /** sonatype oss (for mvn central sync) */
  object sonatype {
    val Host = "oss.sonatype.org"
    val Realm = "Sonatype Nexus Repository Manager"
    val template = templateSrc(Realm, Host)_
  }

  def writeBintray(
    user: String, password: String, path: File) =
    IO.write(path, bintray.template(user, password))

  def writeGitHub(
    user: String, password: String, path: File) =
    IO.write(path, github.template(user, password))

  def writeSonatype(
    user: String, password: String, path: File) =
    IO.write(path, sonatype.template(user, password))

  def readBintray(path: File): Option[BintrayCredentials] =
    readImpl(path, BintrayCredentials(_, _))

  def readGitHub(path: File): Option[GitHubCredentials] =
    readImpl(path, GitHubCredentials(_, _))

  private def readImpl[A <: RepoCredentials](path: File, make: (String, String) => A): Option[A] =
    path match {
      case creds if creds.exists =>
        import scala.collection.JavaConverters._
        val properties = new java.util.Properties
        IO.load(properties, creds)
        val mapped = properties.asScala.map {
          case (k,v) => (k.toString, v.toString.trim)
        }.toMap
        val missing = Keys.filter(!mapped.contains(_))
        if (!missing.isEmpty) None
        else Some(make(mapped("user"), mapped("password")))
      case _ => None
    }
}
