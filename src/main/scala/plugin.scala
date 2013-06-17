package bintray

import sbt._
import bintry._
import dispatch._, dispatch.Defaults

object Opts {
  object resolver {
    val jcenter = MavenRepository("BintrayJCenter", "http://jcenter.bintray.com")
    def bintrayPublisher(name: String, repo: String, pkg: String) =
      MavenRepository(
        "Bintray-%s-%s-%s" format(name, repo, pkg),
        "http://api.bintray.com/maven/%s/%s/%s".format(
          name, repo, pkg))
    def bintrayRepo(name: String, repo: String, pkg: String) =
      MavenRepository(
        "Bintray-%s-%s-%s" format(name, repo, pkg),
        "http://api.bintray.com/maven/%s/%s/%s".format(
          name, repo, pkg))
  }
} 

object Plugin extends sbt.Plugin {
  import Keys._

  val bintrayRepo = SettingKey[String](
    "bintryRepo", "Bintry repository to publish to. Defaults to 'maven'")
  val bintrayPackageLabels = SettingKey[Seq[String]](
    "bintrayPackageLabels", "List of labels associated with bintray package that will be added on auto package creation")

  private def credentialsPath = 
    Path.userHome / ".bintray" / ".credentials"

  private def ensurePackageTask: Def.Initialize[sbt.Task[Unit]] =
    (bintrayRepo, name, description, bintrayPackageLabels, streams).map {
      case (repo, name, desc, labels, out) =>
        ensuredCredentials.map { creds =>
          val bty = Client(creds("user"), creds("password")).repo(creds("user"), repo)
          val exists =
            if (bty.get(name)(new FunctionHandler(_.getStatusCode == 404))()) true
            else bty.createPackage(name, desc, labels:_*)(new FunctionHandler(_.getStatusCode == 201))()
          if (!exists) sys.error("was not able to find or create a package for %s in repo %s named %s"
                                 .format(creds("user"), repo, name))
        }.getOrElse("failed to retrieve bintray credentials")
    }    

  private def publishToBintrayOrDefault: Def.Initialize[Option[Resolver]] =
    (publishTo, bintrayRepo, name, streams).apply {
      case (provided @ Some(_), _, _, out) => provided
      case (_, repo, pkg, out) =>
        ensuredCredentials.map { creds =>
          Opts.resolver.bintrayPublisher(creds("user"), repo, pkg)
        }
    }

  private def mkPackageResolver: Def.Initialize[Resolver] =
    (bintrayRepo, name).apply {
      (repo,  pkg) =>
        ensuredCredentials.map { creds =>
          Opts.resolver.bintrayRepo(creds("user"), repo, pkg)
        }.getOrElse(sys.error("unable to resolve bintray credentials"))
    }

  private def ensuredCredentials =
    readCredentials match {
      case None =>
        val name = SimpleReader.readLine("Enter bintray username: ").get.trim
        if (name.isEmpty) sys.error("bintray user required")
        val pass = SimpleReader.readLine("Enter bintray API key: ", Some('*')).get.trim
        if (pass.isEmpty) sys.error("bintray API key required")

        println("saving credentials to %s" format credentialsPath)
        IO.write(credentialsPath,
          """realm=Bintray
             |host=api.bintray.com
             |user=%s
             |password=%s""".stripMargin.format(name, pass))
        readCredentials
      case creds => creds
    }

  private def readCredentials: Option[Map[String, String]] =
    credentialsPath match {
      case creds if (creds.exists) =>
        import collection.JavaConversions._
        val properties = new java.util.Properties
        IO.load(properties, creds)
        val map = properties map { case (k,v) => (k.toString, v.toString.trim) } toMap
        val keys = Seq("realm", "host", "user", "password")
        val missing = keys.filter(!map.contains(_))
        if (!missing.isEmpty) sys.error("missing credential properties in %s: %s" format(creds, missing.mkString(", ")))
        else Some(map)
      case _ => None
    }

  private def ensureCredentialsTask =
    (streams) map {
      (out) => ensuredCredentials.get
    }

  def bintrayPublishSettings: Seq[Setting[_]] = Seq(
    bintrayRepo := "maven",
    bintrayPackageLabels := Nil,
    publish <<= publish.dependsOn(ensurePackageTask.dependsOn(ensureCredentialsTask)),
    publishTo <<= publishToBintrayOrDefault,
    credentials += Credentials(credentialsPath),
    resolvers <+= mkPackageResolver
  )

  def bintrayResolverSettings: Seq[Setting[_]] = Seq(
    resolvers += Opts.resolver.jcenter
  )

  def bintrySettings: Seq[Setting[_]] = bintrayResolverSettings ++ bintrayPublishSettings
}
