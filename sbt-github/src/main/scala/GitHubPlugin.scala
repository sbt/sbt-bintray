package github

import bintry.Attr
import sbtpackages._
import GitHubKeys._
import sbt.{AutoPlugin, Credentials, Global, Path, Resolver, Setting, Tags, Task, ThisBuild}
import sbt.Classpaths.publishTask
import sbt.Def.{Initialize, setting, task, taskDyn}
import sbt.Keys._
import sbt._

object GitHubPlugin extends AutoPlugin {
  import GitHubPlugin._
  import InternalGitHubKeys._

  override def requires = sbt.plugins.JvmPlugin
  override def trigger = allRequirements

  override def globalSettings: Seq[Setting[_]] = globalPublishSettings
  override def buildSettings: Seq[Setting[_]] = buildPublishSettings
  override def projectSettings: Seq[Setting[_]] = githubSettings

  lazy val isEnabledViaProp: Boolean = sys.props.get("sbt.sbtgithub")
    .getOrElse("true").toLowerCase(java.util.Locale.ENGLISH) match {
    case "true" | "1" | "always" => true
    case _ => false
  }

  object autoImport extends GitHubKeys {
  }

  lazy val Git = Tags.Tag("git")

  def githubSettings: Seq[Setting[_]] =
    githubCommonSettings ++ githubPublishSettings ++ githubQuerySettings

  def githubCommonSettings: Seq[Setting[_]] = Seq(
    githubChangeCredentials := {
      val context = GitHubCredentialContext(githubCredentialsFile.value)
      GitHub.changeCredentials(context, streams.value.log)
    },
    githubWhoami := {
      val context = GitHubCredentialContext(githubCredentialsFile.value)
      GitHub.whoami(GitHub.ensuredCredentials(context, streams.value.log), streams.value.log)
    }
  )

  def bintrayQuerySettings: Seq[Setting[_]] = Seq(
    githubPackageVersions := packageVersionsTask.value
  )

  def globalPublishSettings: Seq[Setting[_]] = Seq(
    githubCredentialsFile in Global := Path.userHome / ".bintray" / ".credentials",
    concurrentRestrictions in Global += Tags.exclusive(Git)
  )

  def buildPublishSettings: Seq[Setting[_]] = Seq(
    githubOrganization in ThisBuild := None,
    githubVcsUrl in ThisBuild := vcsUrlTask.value,
    githubReleaseOnPublish in ThisBuild := true
  )

  def githubPublishSettings: Seq[Setting[_]] = githubCommonSettings ++ Seq(
    githubPackage := moduleName.value,
    githubRepo := GitHub.cachedRepo(githubEnsureCredentials.value,
      githubOrganization.value,
      githubRepository.value),
    // todo: don't force this to be sbt-plugin-releases
    githubRepository := {
      if (sbtPlugin.value) GitHub.defaultSbtPluginRepository
      else GitHub.defaultMavenRepository
    },
    publishMavenStyle := {
      if (sbtPlugin.value) false else publishMavenStyle.value
    },
    githubPackageLabels := Nil,
    description in github := description.value,
    // note: publishTo may not have dependencies. therefore, we can not rely well on inline overrides
    // for inline credentials resolution we recommend defining githubCredentials _before_ mixing in the defaults
    // perhaps we should try overriding something in the publishConfig setting -- https://github.com/sbt/sbt-pgp/blob/master/pgp-plugin/src/main/scala/com/typesafe/sbt/pgp/PgpSettings.scala#L124-L131
    publishTo in github := publishToGitHub.value,
    resolvers in github := {
      val context = GitHubCredentialContext(githubCredentialsFile.value)
      GitHub.buildResolvers(GitHub.ensuredCredentials(context, sLog.value),
        githubOrganization.value,
        githubRepository.value,
        publishMavenStyle.value
      )
    },
    credentials in github := {
      Seq(githubCredentialsFile.value).filter(_.exists).map(Credentials.apply)
    },
    githubPackageAttributes := {
      if (sbtPlugin.value) Map(AttrNames.sbtPlugin -> Seq(Attr.Boolean(sbtPlugin.value)))
      else Map.empty
    },
    githubVersionAttributes := {
      val scalaVersions = crossScalaVersions.value
      val sv = Map(AttrNames.scalas -> scalaVersions.map(Attr.Version))
      if (sbtPlugin.value) sv ++ Map(AttrNames.sbtVersion-> Seq(Attr.Version(sbtVersion.value)))
      else sv
    },
    githubOmitLicense := {
      if (sbtPlugin.value) sbtPlugin.value
      else false
    },
    githubEnsureLicenses := {
      GitHub.ensureLicenses(licenses.value, githubOmitLicense.value)
    },
    githubEnsureCredentials := {
      val context = GitHubCredentialContext(githubCredentialsFile.value)
      GitHub.ensuredCredentials(context, streams.value.log).getOrElse {
        sys.error(s"Missing github credentials. " +
          s"Either create a credentials file with the githubChangeCredentials task, " +
          s"set the GITHUB_USER and BINTRAY_PASS environment variables or " +
          s"pass github.user and github.pass properties to sbt.")
      }
    },
    githubEnsureGitHubPackageExists := ensurePackageTask.value,
    githubUnpublish := dynamicallyGitHubUnpublish.value,
    githubRemoteSign := {
      val repo = githubRepo.value
      repo.remoteSign(githubPackage.value, version.value, streams.value.log)
    },
    githubSyncMavenCentral := syncMavenCentral(close = true).value,
    githubSyncSonatypeStaging := syncMavenCentral(close = false).value,
    githubSyncMavenCentralRetries := Seq.empty,
    githubRelease := {
      val _ = publishVersionAttributesTask.value
      val repo = githubRepo.value
      repo.release(githubPackage.value, version.value, streams.value.log)
    }
  ) ++ Seq(
    resolvers ++= {
      val rs = (resolvers in github).value
      if (isEnabledViaProp) rs
      else Nil
    },
    credentials ++= {
      val cs = (credentials in github).value
      if (isEnabledViaProp) cs
      else Nil
    },
    publishTo := {
      val old = publishTo.value
      val p = (publishTo in github).value
      if (isEnabledViaProp) p
      else old
    },
    publish := dynamicallyPublish.value
  )

  private def syncMavenCentral(close: Boolean): Initialize[Task[Unit]] = task {
    val repo = githubRepo.value
    repo.syncMavenCentral(githubPackage.value, version.value, credentials.value, close, githubSyncMavenCentralRetries.value, streams.value.log)
  }

  private def vcsUrlTask: Initialize[Task[Option[String]]] =
    task {
      GitHub.resolveVcsUrl.recover { case _ => None }.get
    }.tag(Git)

  // uses taskDyn because it can return one of two potential tasks
  // as its result, each with their own dependencies
  // see also: http://www.scala-sbt.org/0.13/docs/Tasks.html#Dynamic+Computations+with
  private def dynamicallyPublish: Initialize[Task[Unit]] =
    taskDyn {
      val sk = ((skip in publish) ?? false).value
      val s = streams.value
      val ref = thisProjectRef.value

      if (!isEnabledViaProp) publishTask(publishConfiguration, deliver)
      else if (sk) Def.task {
        s.log.debug(s"skipping publish for ${ref.project}")
      }
      else dynamicallyPublish0
    }

  private def dynamicallyPublish0: Initialize[Task[Unit]] =
    taskDyn {
      (if (githubReleaseOnPublish.value) githubRelease else warnToRelease).dependsOn(publishTask(publishConfiguration, deliver))
    } dependsOn(githubEnsureGitHubPackageExists, githubEnsureLicenses)

  // uses taskDyn because it can return one of two potential tasks
  // as its result, each with their own dependencies
  // see also: http://www.scala-sbt.org/0.13/docs/Tasks.html#Dynamic+Computations+with
  private def dynamicallyGitHubUnpublish: Initialize[Task[Unit]] =
    taskDyn {
      val repo = githubRepo.value
      val sk = ((skip in publish) ?? false).value
      val s = streams.value
      val ref = thisProjectRef.value
      if (sk) Def.task {
        s.log.debug(s"Skipping githubUnpublish for ${ref.project}")
      } else dynamicallyGitHubUnpublish0
    }

  private def dynamicallyGitHubUnpublish0: Initialize[Task[Unit]] =
    Def.task {
      val repo = githubRepo.value
      repo.unpublish(githubPackage.value, version.value, streams.value.log)
    }.dependsOn(githubEnsureBintrayPackageExists, githubEnsureLicenses)

  private def warnToRelease: Initialize[Task[Unit]] =
    task {
      val log = streams.value.log
      log.warn("You must run bintrayRelease once all artifacts are staged.")
    }

  private def publishVersionAttributesTask: Initialize[Task[Unit]] =
    task {
      val repo = githubRepo.value
      repo.publishVersionAttributes(
        githubPackage.value,
        version.value,
        githubVersionAttributes.value)
    }

  private def ensurePackageTask: Initialize[Task[Unit]] =
    task {
      val vcs = githubVcsUrl.value.getOrElse {
        sys.error("""githubVcsUrl not defined. assign this with githubVcsUrl := Some("git@github.com:you/your-repo.git")""")
      }
      val repo = githubRepo.value
      repo.ensurePackage(githubPackage.value,
        githubPackageAttributes.value,
        (description in github).value,
        vcs,
        licenses.value,
        githubPackageLabels.value,
        streams.value.log)
    }

  /** set a user-specific bintray endpoint for sbt's `publishTo` setting.*/
  private def publishToGitHub: Initialize[Option[Resolver]] =
    setting {
      val credsFile = githubCredentialsFile.value
      val btyOrg = githubOrganization.value
      val repoName = githubRepository.value
      val context = GitHubCredentialContext(credsFile)
      // ensure that we have credentials to build a resolver that can publish to github
      GitHub.withRepo(context, btyOrg, repoName, sLog.value) { repo =>
        repo.buildPublishResolver(githubPackage.value,
          version.value,
          publishMavenStyle.value,
          sbtPlugin.value,
          githubReleaseOnPublish.value,
          sLog.value
        )
      }
    }

  /** Lists versions of bintray packages corresponding to the current project */
  private def packageVersionsTask: Initialize[Task[Seq[String]]] =
    task {
      val credsFile = githubCredentialsFile.value
      val btyOrg = githubOrganization.value
      val repoName = githubRepository.value
      val context = GitHubCredentialContext(credsFile)
      GitHub.withRepo(context, btyOrg, repoName, streams.value.log) { repo =>
        repo.packageVersions(githubPackage.value, streams.value.log)
      }.getOrElse(Nil)
    }
}
