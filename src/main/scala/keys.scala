package bintray

import sbt._

object Keys {
  val bintray = TaskKey[String](
    "bintray", "bintray-sbt is an interface for the bintray package service")

  val repository = SettingKey[String](
    "repository", "Bintray repository to publish to. Defaults to 'maven'")

  val packageLabels = SettingKey[Seq[String]](
    "packageLabels", "List of labels associated with your bintray package")

  val packageAttributes = SettingKey[AttrMap](
    "packageAttributes", "List of bintray package metadata attributes")

  val versionAttributes = SettingKey[AttrMap](
    "versionAttributes", "List of bintray version metadata attributes")

  val credentialsFile = SettingKey[File](
    "credentialsFile", "File containing bintray api credentials")

  val packageVersions = TaskKey[Seq[String]](
    "packageVersions", "List bintray versions for the current package")

  val changeCredentials = TaskKey[Unit](
    "changeCredentials", "Change your current bintray credentials")

  /** named used for common package attributes lifted from sbt
   *  build definitions */
  object AttrNames {
    val scalaVersion = "scala-version"
    val sbtPlugin = "sbt-plugin"
    val sbtVersion = "sbt-version"
  }
}
