package bintray

import sbt.IO
import java.io.File

case class BintrayCredentials(
  user: String, password: String) {
  override def toString = s"BintrayCredentials($user, ${"x"*password.length})"
}

object BintrayCredentials {

  val Keys = Seq("realm", "host", "user", "password")
  def templateSrc(realm: String, host: String)(
    name: String, password: String) =
    s"""realm = $realm
       |host = $host
       |user = $name
       |password = $password""".stripMargin

  /** bintray api */
  object api {
    def toDirect(bc: BintrayCredentials) =
      sbt.Credentials(Realm, Host, bc.user, bc.password)
    val Host = "api.bintray.com"
    val Realm = "Bintray API Realm"
    val template = templateSrc(Realm, Host)_
  }

  /** sonatype oss (for mvn central sync) */
  object sonatype {
    val Host = "oss.sonatype.org"
    val Realm = "Sonatype Nexus Repository Manager"
    val template = templateSrc(Realm, Host)_
  }

  def read(path: File): Either[String,Option[BintrayCredentials]] =
    path match {
      case creds if creds.exists =>
        import collection.JavaConverters._
        val properties = new java.util.Properties
        IO.load(properties, creds)
        val mapped = properties.asScala.map {
          case (k,v) => (k.toString, v.toString.trim)
        }.toMap
        val missing = Keys.filter(!mapped.contains(_))
        if (missing.nonEmpty) Left(
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
