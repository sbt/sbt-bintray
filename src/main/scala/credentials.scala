package bintray

import sbt.IO
import java.io.File

object BintrayCredentials {
  val Keys = Seq("realm", "host", "user", "password")
  def template(name: String, password: String) =
    """realm = Bintray
      |host = api.bintray.com
      |user = %s
      |password = %s""".stripMargin.format(name, password)

  def read(path: File): Either[String,Option[Map[String, String]]] =
    path match {
      case creds if (creds.exists) =>
        import collection.JavaConversions._
        val properties = new java.util.Properties
        IO.load(properties, creds)
        val mapped = properties.map {
          case (k,v) => (k.toString, v.toString.trim)
        }.toMap
        val missing = Keys.filter(!mapped.contains(_))
        if (!missing.isEmpty) Left(
          "missing credential properties in %s: %s"
            .format(creds, missing.mkString(", ")))
        else Right(Some(mapped))
      case _ => Right(None)
    }
    
  def write(user: String, password: String, path: File) =
    IO.write(path, template(user, password))
}
