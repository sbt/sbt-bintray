package sbtbintrayremotecache

import sbt._

import scala.concurrent.duration._

trait BintrayRemoteCacheKeys {
  val bintrayRemoteCacheCredentialsFile = settingKey[File](
    "File containing bintray api credentials")
  val bintrayRemoteCacheOrganization = settingKey[String](
    "Bintray organization name to push to")
  val bintrayRemoteCacheRepository = settingKey[String](
    "Bintray repository to publish to (default: remote-cache)")
  val bintrayRemoteCachePackage = settingKey[String](
    "Bintray package name")
  val bintrayRemoteCacheCleanOld = taskKey[Unit](
    "Clean old remote cache")
  val bintrayRemoteCacheMinimum = settingKey[Int](
    s"Minimum number of cache to keep around (default: ${BintrayRemoteDefaults.minimum})")
  val bintrayRemoteCacheTtl = settingKey[Duration](
    s"Time to keep remote cache around (default: ${BintrayRemoteDefaults.ttl})")
}

object BintrayRemoteCacheKeys extends BintrayRemoteCacheKeys

object BintrayRemoteDefaults {
  def minimum: Int = 100
  def ttl: Duration = Duration(30, DAYS)
}
