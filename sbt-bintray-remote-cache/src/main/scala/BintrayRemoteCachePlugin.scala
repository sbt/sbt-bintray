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
  )

  override lazy val projectSettings: Seq[Setting[_]] = Seq(
    pushRemoteCacheTo := publishToBintray.value,
  )

  def publishToBintray =
    Def.setting {
      val credsFile = bintrayRemoteCacheCredentialsFile.value
      val btyOrg = bintrayRemoteCacheOrganization.value
      val repoName = bintrayRemoteCacheRepository.value
      val context = BintrayCredentialContext.remoteCache(credsFile)
      Bintray.withRepo(context, Some(btyOrg), repoName, sLog.value) { repo =>
        repo.buildRemoteCacheResolver(bintrayRemoteCachePackage.value, sLog.value)
      }
    }
}
