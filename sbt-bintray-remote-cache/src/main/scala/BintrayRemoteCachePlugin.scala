package sbtbintrayremotecache

import sbt._
import Keys._
import bintray._

object BintrayRemoteCachePlugin extends AutoPlugin {
  override def requires = sbt.plugins.JvmPlugin
  override def trigger = allRequirements

  object autoImport extends BintrayRemoteCacheKeys
  import autoImport._

  override lazy val globalSettings: Seq[Setting[_]] = Seq(
    bintrayRemoteCacheCredentialsFile := Path.userHome / ".bintray" / ".credentials",
    bintrayRemoteCacheRepository := "remote-cache",
    bintrayRemoteCacheMinimum := BintrayRemoteDefaults.minimum,
    bintrayRemoteCacheTtl := BintrayRemoteDefaults.ttl,
  )

  override lazy val buildSettings: Seq[Setting[_]] = Seq(
    bintrayRemoteCacheCleanOld := packageCleanOldVersionsTask.value,
    bintrayRemoteCacheCleanOld / aggregate := false,
  )

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    pushRemoteCacheTo := publishToBintraySetting.value,
    remoteCacheResolvers := {
      val btyOrg = bintrayRemoteCacheOrganization.value
      val repoName = bintrayRemoteCacheRepository.value
      List(Resolver.bintrayRepo(btyOrg, repoName))
    },
  )

  def publishToBintraySetting =
    Def.setting {
      val credsFile = bintrayRemoteCacheCredentialsFile.value
      val btyOrg = bintrayRemoteCacheOrganization.value
      val repoName = bintrayRemoteCacheRepository.value
      val context = BintrayCredentialContext.remoteCache(credsFile)
      Bintray.withRepo(context, Some(btyOrg), repoName, sLog.value) { repo =>
        repo.buildRemoteCacheResolver(bintrayRemoteCachePackage.value, sLog.value)
      }
    }

  def packageCleanOldVersionsTask: Def.Initialize[Task[Unit]] =
    Def.task {
      val credsFile = bintrayRemoteCacheCredentialsFile.value
      val btyOrg = bintrayRemoteCacheOrganization.value
      val repoName = bintrayRemoteCacheRepository.value
      val context = BintrayCredentialContext.remoteCache(credsFile)
      val pkg = bintrayRemoteCachePackage.value
      val s = streams.value
      val min = bintrayRemoteCacheMinimum.value
      val ttl = bintrayRemoteCacheTtl.value
      Bintray.withRepo(context, Some(btyOrg), repoName, s.log) { repo =>
        repo.cleandOldVersions(pkg, min, ttl, s.log)
      }
    }
}
