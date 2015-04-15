package bintray

import bintry.Attr
import sbt.{ Credentials, Global, Path, Resolver, Setting, Task }
import sbt.Def.{ Initialize, setting, task }
import sbt.Keys._
import sbt.Path.richFile
import scala.util.control.NonFatal

object Plugin extends sbt.Plugin {
  import bintray.Keys._

  /** publishes version meta attributes after successfully publishing artifact files. this happens _after_
   *  a successful publish action */
  private def publishWithVersionAttrs: Initialize[Task[Unit]] =
    (publish, publishVersionAttributes)(_ && _)

  /** updates a package version with the values defined in versionAttributes in bintray */
  private def publishVersionAttributesTask: Initialize[Task[Unit]] =
    task {
      val pkg = (name in bintray).value
      val repo = BintrayRepo(ensureCredentials.value,
          (bintrayOrganization in bintray).value,
          (repository in bintray).value)
      repo.publishVersionAttributes(
        pkg, version.value, (versionAttributes in bintray).value)
    }

  /** Ensure user-specific bintray package exists. This will have a side effect of updating the packages attrs
   *  when it exists.
   *  todo(doug): Perhaps we want to factor that into an explicit task. */
  private def ensurePackageTask: Initialize[Task[Unit]] =
    task {
      val pkg = (name in bintray).value
      val vcs = (vcsUrl in bintray).value.getOrElse {
        sys.error("""vcsUrl not defined. assign this with (vcsUrl in bintray) := Some("git@github.com:you/your-repo.git")""")
      }
      val repo = BintrayRepo(ensureCredentials.value,
          (bintrayOrganization in bintray).value,
          (repository in bintray).value)
      repo.ensurePackage(pkg,
        (packageAttributes in bintray).value,
        (description in bintray).value,
        vcs,
        licenses.value,
        (packageLabels in bintray).value)
    }

  /** set a user-specific bintray endpoint for sbt's `publishTo` setting.*/
  private def publishToBintray: Initialize[Option[Resolver]] =
    setting {
      val credsFile = (credentialsFile in bintray).value
      val btyOrg = (bintrayOrganization in bintray).value
      val repoName = (repository in bintray).value
      val pkg = (name in bintray).value

      // ensure that we have credentials to build a resolver that can publish to bintray
      Bintray.withRepo(credsFile, btyOrg, repoName) { repo =>
        repo.buildPublishResolver(pkg,
          version.value,
          publishMavenStyle.value,
          sbtPlugin.value)
      }
    }

  /** unpublish (delete) a version of a package */
  private def unpublishTask: Initialize[Task[Unit]] =
    task {
      val pkg = (name in bintray).value
      val repo = BintrayRepo(ensureCredentials.value,
        (bintrayOrganization in bintray).value,
        (repository in bintray).value)
      repo.unpublish(pkg, version.value, sLog.value)
    }

  /** pgp sign remotely published artifacts then publish those signed artifacts.
   *  this assumes artifacts are published remotely. signing artifacts doesn't
   *  mean the signings themselves will be published so it is nessessary to publish
   *  this immediately after.
   */
  private def remoteSignTask: Initialize[Task[Unit]] =
    task {
      val pkg = (name in bintray).value
      val repo = BintrayRepo(ensureCredentials.value,
          (bintrayOrganization in bintray).value,
          (repository in bintray).value)
      repo.remoteSign(pkg, version.value, sLog.value)
    }

  /** synchronize a published set of artifacts for a pkg version to mvn central
   *  this requires already having a sonatype oss account set up.
   *  this is itself quite a task but in the case the user has done this in the past
   *  this can be quiet a convenient feature */
  private def syncMavenCentralTask: Initialize[Task[Unit]] =
    task {
      val pkg = (name in bintray).value
      val repo = BintrayRepo(ensureCredentials.value,
          (bintrayOrganization in bintray).value,
          (repository in bintray).value)
      repo.syncMavenCentral(pkg, version.value, credentials.value, sLog.value)
    }

  /** if credentials exist, append a user-specific resolver */
  private def appendBintrayResolver: Initialize[Seq[Resolver]] =
    setting {
      val creds = (credentialsFile in bintray).value
      val btyOrg = (bintrayOrganization in bintray).value
      val repo = (repository in bintray).value
      BintrayCredentials.read(creds).fold({ err =>
        println(s"bintray credentials $err is malformed")
        Nil
      }, {
        _.map { case BintrayCredentials(user, _) => Seq(Opts.resolver.repo(btyOrg.getOrElse(user), repo)) }
         .getOrElse(Nil)
      })
    }

  /** Lists versions of bintray packages corresponding to the current project */
  private def packageVersionsTask: Initialize[Task[Seq[String]]] =
    task {
      val credsFile = (credentialsFile in bintray).value
      val btyOrg = (bintrayOrganization in bintray).value
      val repoName = (repository in bintray).value
      val pkg = (name in bintray).value
      (Bintray.withRepo(credsFile, btyOrg, repoName) { repo =>
        repo.packageVersions(pkg, sLog.value)
      }).getOrElse(Nil)
    }

  /** assign credentials or ask for new ones */
  private def changeCredentialsTask: Initialize[Task[Unit]] =
    (credentialsFile in bintray).map { Bintray.changeCredentials(_) }

  /** log who bintry sbt thinks you are with respect to bintray api authorization */
  private def whoamiTask: Initialize[Task[String]] =
    task {
      Bintray.whoami((credentialsFile in bintray).value, sLog.value)
    }

  private def ensureCredentialsTask: Initialize[Task[BintrayCredentials]] =
    task {
      Bintray.ensuredCredentials(
        (credentialsFile in bintray).value).get
    }

  /** publishing to bintray requires you must have defined a license they support */
  private def ensureLicensesTask: Initialize[Task[Unit]] =
    task {
      Bintray.ensureLicenses(licenses.value, omitLicense.value)
    }

  def bintrayPublishSettings: Seq[Setting[_]] = bintrayCommonSettings ++ Seq(
    credentialsFile in bintray in Global := Path.userHome / ".bintray" / ".credentials",
    name in bintray := moduleName.value,
    bintrayOrganization in bintray in Global := { if (sbtPlugin.value) Some("sbt") else None },
    // todo: don't force this to be sbt-plugin-releases
    repository in bintray in Global := { if (sbtPlugin.value) "sbt-plugin-releases" else "maven" },
    publishMavenStyle := {
      if (sbtPlugin.value) false else publishMavenStyle.value
    },
    vcsUrl in bintray := Bintray.resolveVcsUrl.recover {
      case NonFatal(e) =>
        None
    }.get,
    packageLabels in bintray := Nil,
    description in bintray <<= description,
    // note: publishTo may not have dependencies. therefore, we can not rely well on inline overrides
    // for inline credentials resolution we recommend defining bintrayCredentials _before_ mixing in the defaults
    // perhaps we should try overriding something in the publishConfig setting -- https://github.com/sbt/sbt-pgp/blob/master/pgp-plugin/src/main/scala/com/typesafe/sbt/pgp/PgpSettings.scala#L124-L131
    publishTo in bintray <<= publishToBintray,
    resolvers in bintray <<= appendBintrayResolver,
    credentials in bintray <<= (credentialsFile in bintray).map {
      Credentials(_) :: Nil
    },
    packageAttributes in bintray <<= (sbtPlugin, sbtVersion) {
      (plugin, sbtVersion) =>
        if (plugin) Map(AttrNames.sbtPlugin -> Seq(Attr.Boolean(plugin)))
        else Map.empty
    },
    versionAttributes in bintray <<= (crossScalaVersions, sbtPlugin, sbtVersion) {
      (scalaVersions, plugin, sbtVersion) =>
        val sv = Map(AttrNames.scalas -> scalaVersions.map(Attr.Version(_)))
        if (plugin) sv ++ Map(AttrNames.sbtVersion-> Seq(Attr.Version(sbtVersion))) else sv
    },
    omitLicense in bintray in Global := { if (sbtPlugin.value) sbtPlugin.value else false },
    ensureLicenses <<= ensureLicensesTask,
    ensureCredentials <<= ensureCredentialsTask,
    ensureBintrayPackageExists <<= ensurePackageTask,
    publishVersionAttributes <<= publishVersionAttributesTask,
    unpublish in bintray <<= unpublishTask.dependsOn(ensureBintrayPackageExists, ensureLicenses),
    remoteSign in bintray <<= remoteSignTask,
    syncMavenCentral in bintray <<= syncMavenCentralTask
  ) ++ Seq(
    resolvers <++= resolvers in bintray,
    credentials <++= credentials in bintray,
    publishTo <<= publishTo in bintray,
    // We attach this to publish configruation, so that publish-signed in pgp plugin can work.
    publishConfiguration <<= publishConfiguration.dependsOn(ensureBintrayPackageExists, ensureLicenses), 
    publish <<= publishWithVersionAttrs
  )

  def bintrayCommonSettings: Seq[Setting[_]] = Seq(
    changeCredentials in bintray <<= changeCredentialsTask,
    whoami in bintray <<= whoamiTask
  )

  def bintrayQuerySettings: Seq[Setting[_]] = Seq(
    packageVersions in bintray <<= packageVersionsTask
  )

  @deprecated("use resolvers += sbt.Resolver.jcenterRepo instead. (available in sbt 0.13.6+)", since="0.2.0")
  def bintrayResolverSettings: Seq[Setting[_]] = Seq(
    resolvers += Opts.resolver.jcenter
  )

  def bintraySettings: Seq[Setting[_]] =
    bintrayCommonSettings ++ bintrayResolverSettings ++ bintrayPublishSettings ++ bintrayQuerySettings
}
