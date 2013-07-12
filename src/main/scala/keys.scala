package bintray

import sbt._

object Keys {
  val bintray = TaskKey[String](
    "bintray", "bintray-sbt")

  val repository = SettingKey[String](
    "repository", "Bintray repository to publish to. Defaults to 'maven'")

  val packageLabels = SettingKey[Seq[String]](
    "packageLabels", "List of labels associated with your bintray package")

  val credentialsFile = SettingKey[File](
    "credentialsFile", "File containing bintray api credentials")

  val packageVersions = TaskKey[Seq[String]](
    "packageVersions", "List bintray versions for the current package")

  val changeCredentials = TaskKey[Unit](
    "changeCredentials", "Change your current bintray credentials")
}
