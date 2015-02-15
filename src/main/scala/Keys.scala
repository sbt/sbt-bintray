package bintray

import sbt._

object Keys {
  val bintray = TaskKey[String](
    "bintray", "bintray-sbt is an interface for the bintray package service")

  val bintrayOrganization = SettingKey[Option[String]](
    "bintrayOrganization", "Bintray organization name to publish to. Defaults to None unless project is an sbtPlugin")

  val repository = SettingKey[String](
    "repository", "Bintray repository to publish to. Defaults to 'maven' unless project is an sbtPlugin")

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

  val whoami = TaskKey[String](
    "whoami", "Print the name of the currently authenticated bintray user")

  val omitLicense = SettingKey[Boolean](
     "omitLicense", "Omit license, useful if publishing to a private repo. Defaults to false")

  val ensureLicenses = TaskKey[Unit](
    "bintrayEnsureLicenses", "Ensure that the licenses for bintray are valid.")

   val ensureCredentials = TaskKey[BintrayCredentials](
    "bintrayEnsureCredentials", "Ensure that the credentials for bintray are valid.")

  val ensureBintrayPackageExists = TaskKey[Unit](
    "bintrayEnsurePackage", "Ensure that the bintray package exists and is valid.")

  val publishVersionAttributes = TaskKey[Unit](
    "bintrayPublishVersionAttributes", "Publish the attributes for the current version of this package to bintray.")

  val unpublish = TaskKey[Unit](
    "unpublish", "Unpublishes a version of package on bintray")

  val remoteSign = TaskKey[Unit](
    "remoteSign", "PGP sign artifacts hosted remotely on bintray. (See also https://bintray.com/docs/uploads/uploads_gpgsigning.html)")

  val syncMavenCentral = TaskKey[Unit](
    "syncMavenCentral", "Sync bintray-published artifacts with maven central")

  /** named used for common package attributes lifted from sbt
   *  build definitions */
  object AttrNames {
    val scalas = "scalas"
    val sbtPlugin = "sbt-plugin"
    val sbtVersion = "sbt-version"
  }
}
