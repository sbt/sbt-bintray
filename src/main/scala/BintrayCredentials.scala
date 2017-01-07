package bintray

import sbt.IO
import java.io.File

case class BintrayCredentials(
  user: String, password: String) {
  override def toString = s"BintrayCredentials($user, ${"x"*password.size})"
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

  def fileCredentials(path: File) =
    path match {
      case creds if creds.exists =>
        import collection.JavaConversions._
        val properties = new java.util.Properties
        IO.load(properties, creds)
        val mapped = properties.map {
          case (k,v) => (k.toString, v.toString.trim)
        }.toMap
        val missing = Keys.filter(!mapped.contains(_))
        if (!missing.isEmpty) None
        else Some(mapped("user"), mapped("password"))
      case _ => None
    }

  def cachedCredentials(key: String) = {
    val cached = Cache.getMulti(s"$key.user", s"$key.pass")
    (cached(s"$key.user"), cached(s"$key.pass")) match {
      case (Some(user), Some(pass)) => Some((user, pass))
      case _ => None
    }
  }

  def propsCredentials(key: String) = {
    for {
      name <- sys.props.get(s"$key.user")
      pass <- sys.props.get(s"$key.pass")
    } yield (name, pass)
  }

  def envCredentials(key: String) = {
    for {
      name <- sys.env.get(s"${key.toUpperCase}_USER")
      pass <- sys.env.get(s"${key.toUpperCase}_PASS")
    } yield (name, pass)
  }

  def promptCredentials(key: String) = {
    for {
      name <- Prompt(s"Enter $key username")
      pass <- Prompt.descretely(s"Enter $key password")
    } yield (name, pass)
  }

  def writeBintray(
    user: String, password: String, path: File) =
    IO.write(path, api.template(user, password))

  def writeSonatype(
    user: String, password: String, path: File) =
    IO.write(path, sonatype.template(user, password))
}
