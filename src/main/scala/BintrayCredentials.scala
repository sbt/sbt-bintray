package bintray

import sbt.IO
import java.io.File

case class BintrayCredentials(user: String, password: String)

object BintrayCredentials {

  val Keys = Seq("realm", "host", "user", "password")
  def templateSrc(realm: String, host: String)(
    name: String, password: String) =
    """realm = %s
      |host = %s
      |user = %s
      |password = %s""".stripMargin.format(
        realm, host, name, password)

  object api {
    def toDirect(bc: BintrayCredentials) =
      sbt.Credentials(Realm, Host, bc.user, bc.password)
    val Host = "api.bintray.com"
    val Realm = "Bintray API Realm"
    val template = templateSrc(Realm, Host)_
  }

  object sonatype {
    val Host = "oss.sonatype.org"
    val Realm = "Sonatype Nexus Repository Manager"
    val template = templateSrc(Realm, Host)_
  }

  def read(path: File): Either[String,Option[BintrayCredentials]] =
    path match {
      case creds if creds.exists =>
        import collection.JavaConversions._
        val properties = new java.util.Properties
        IO.load(properties, creds)
        val mapped = properties.map {
          case (k,v) => (k.toString, v.toString.trim)
        }.toMap
        val missing = Keys.filter(!mapped.contains(_))
        if (!missing.isEmpty) Left(
          "missing credential properties %s in %s"
            .format(missing.mkString(", "), creds))
        else Right(Some(BintrayCredentials(
          mapped("user"), mapped("password"))))
      case _ => Right(None)
    }

  def writeBintray(
    user: String, password: String, path: File) =
    IO.write(path, api.template(user, password))

  def writeSonatype(
    user: String, password: String, path: File) =
    IO.write(path, sonatype.template(user, password))
}
