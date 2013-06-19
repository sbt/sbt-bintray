package bintray

import java.io.File
import sbt._
import bintry._
import dispatch._, dispatch.Defaults

object Plugin extends sbt.Plugin {
  import Keys._

  val bintrayRepo = SettingKey[String](
    "bintrayRepo", "Bintry repository to publish to. Defaults to 'maven'")
  val bintrayPackageLabels = SettingKey[Seq[String]](
    "bintrayPackageLabels", "List of labels associated with bintray package that will be added on auto package creation")
  val bintrayCredentialsPath = SettingKey[File](
    "bintrayCredentialsPath", "File containing bintray api credentials")

  /** Ensure user-specific bintray package exists */
  private def ensurePackageTask: Def.Initialize[sbt.Task[Unit]] =
    (bintrayCredentialsPath, bintrayRepo, name, description, bintrayPackageLabels, streams).map {
      case (creds, repo, name, desc, labels, out) =>
        ensuredCredentials(creds).map {
          case BintrayCredentials(user, key) =>
            val bty = Client(user, key).repo(user, repo)
            val exists =
              if (bty.get(name)(new FunctionHandler(_.getStatusCode != 404))()) true
              else bty.createPackage(name, desc, labels:_*)(new FunctionHandler(_.getStatusCode == 201))()
            if (!exists) sys.error("was not able to find or create a package for %s in repo %s named %s"
                                   .format(user, repo, name))
        }.getOrElse("failed to retrieve bintray credentials")
    }

  /** if credentials exists and the build user hasn't defined a publishTo,
   *  set a user-speciic publishTo endpoint */
  private def publishToBintrayOrDefault: Def.Initialize[Option[Resolver]] =
    (publishTo, bintrayCredentialsPath, bintrayRepo, name, streams).apply {
      case (provided @ Some(_), _, _, _, out) => provided
      case (_, creds, repo, pkg, out) =>
        ensuredCredentials(creds, prompt = false).map {
          case BintrayCredentials(user, _) => Opts.resolver.publishTo(user, repo, pkg)
        }
    }

  /** if credentials exist, append a user-specific resolver */
  private def appendBintrayResolver: Def.Initialize[Seq[Resolver]] =
    (bintrayCredentialsPath, bintrayRepo).apply {
      (creds, repo) =>
        BintrayCredentials.read(creds).fold({ err =>
          println("bintray credentials %s is malformed".format(err))
          Nil
        }, {
          _.map { case BintrayCredentials(user, _) => Seq(Opts.resolver.repo(user, repo)) }
           .getOrElse(Nil)
        })
    }

  private def ensuredCredentials(creds: File, prompt: Boolean = true): Option[BintrayCredentials] =
    BintrayCredentials.read(creds).fold(sys.error(_), _ match {
      case None =>
        if (prompt) {
          println("bintray-sbt requires your bintray credentials.")
          val name = Prompt("Enter bintray username")
          if (!name.isDefined) sys.error("bintray username required")
          val pass = Prompt.descretely("Enter bintray API key")
          if (!pass.isDefined) sys.error("bintray API key required")

          Seq(name, pass).flatten match {
            case Seq(name, pass) =>
              println("saving credentials to %s" format creds)
              BintrayCredentials.write(name, pass, creds)
              ensuredCredentials(creds, prompt)
          }
        } else {
          println("Missing bintray credentials %s. Some bintray features depend on this." format creds)
          None
        }
      case creds => creds
    })

  private def ensureCredentialsTask =
    (streams, bintrayCredentialsPath) map {
      (out, creds) => ensuredCredentials(creds).get
    }

  def bintrayPublishSettings: Seq[Setting[_]] = Seq(
    bintrayCredentialsPath := Path.userHome / ".bintray" / ".credentials",
    bintrayRepo := "maven",
    bintrayPackageLabels := Nil,
    publishTo <<= publishToBintrayOrDefault,
    resolvers <++= appendBintrayResolver,
    publish <<= publish.dependsOn(ensurePackageTask.dependsOn(ensureCredentialsTask)),
    credentials <+= bintrayCredentialsPath.map(Credentials(_))
  )

  def bintrayResolverSettings: Seq[Setting[_]] = Seq(
    resolvers += Opts.resolver.jcenter
  )

  def bintrySettings: Seq[Setting[_]] = bintrayResolverSettings ++ bintrayPublishSettings
}
