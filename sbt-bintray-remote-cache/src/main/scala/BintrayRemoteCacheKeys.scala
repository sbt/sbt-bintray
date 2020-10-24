package sbtbintrayremotecache

import sbt._

import scala.concurrent.duration.Duration

trait BintrayRemoteCacheKeys {
  val bintrayRemoteCacheCredentialsFile = settingKey[File](
    "File containing bintray api credentials")
  val bintrayRemoteCacheOrganization = settingKey[String](
    "Bintray organization name to push to.")
  val bintrayRemoteCacheRepository = settingKey[String](
    "Bintray repository to publish to. Defaults to 'remote-cache'.")
  val bintrayRemoteCachePackage = settingKey[String](
    "Bintray package name")
}

object BintrayRemoteCacheKeys extends BintrayRemoteCacheKeys
