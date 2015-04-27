package bintray

import sbt._

trait BintrayKeys {
  val bintray = taskKey[String]("bintray-sbt is an interface for the bintray package service")

  val bintrayRelease = taskKey[Unit](
    "Releases a version of package on bintray")

  val bintrayReleaseOnPublish = settingKey[Boolean](
    "When set to true, publish also runs bintrayRelease.")

  val bintrayOrganization = settingKey[Option[String]](
    "Bintray organization name to publish to. Defaults to None unless project is an sbtPlugin")

  val bintrayPackage = settingKey[String](
    "Bintray package name")

  val bintrayRepository = settingKey[String](
    "Bintray repository to publish to. Defaults to 'maven' unless project is an sbtPlugin")

  val bintrayPackageLabels = settingKey[Seq[String]](
    "List of labels associated with your bintray package")

  val bintrayPackageAttributes = settingKey[AttrMap](
    "List of bintray package metadata attributes")

  val bintrayVersionAttributes = settingKey[AttrMap](
    "List of bintray version metadata attributes")

  val bintrayCredentialsFile = settingKey[File](
    "File containing bintray api credentials")

  val bintrayPackageVersions = taskKey[Seq[String]](
    "List bintray versions for the current package")

  val bintrayChangeCredentials = taskKey[Unit](
    "Change your current bintray credentials")

  val bintrayWhoami = taskKey[String](
    "Print the name of the currently authenticated bintray user")

  val bintrayOmitLicense = settingKey[Boolean](
     "Omit license, useful if publishing to a private repo. Defaults to false")

  val bintrayEnsureLicenses = taskKey[Unit](
    "Ensure that the licenses for bintray are valid.")

  val bintrayEnsureCredentials = taskKey[BintrayCredentials](
    "Ensure that the credentials for bintray are valid.")

  val bintrayEnsureBintrayPackageExists = taskKey[Unit](
    "Ensure that the bintray package exists and is valid.")

  val bintrayUnpublish = taskKey[Unit](
    "Unpublishes a version of package on bintray")

  val bintrayRemoteSign = taskKey[Unit](
    "PGP sign artifacts hosted remotely on bintray. (See also https://bintray.com/docs/uploads/uploads_gpgsigning.html)")

  val bintraySyncMavenCentral = taskKey[Unit](
    "Sync bintray-published artifacts with maven central")

  val bintrayVcsUrl = taskKey[Option[String]](
    "Canonical url for hosted version control repository")

  /** named used for common package attributes lifted from sbt
   *  build definitions */
  object AttrNames {
    val scalas = "scalas"
    val sbtPlugin = "sbt-plugin"
    val sbtVersion = "sbt-version"
  }
}

object BintrayKeys extends BintrayKeys {}

trait InternalBintrayKeys {
  val bintrayRepo = taskKey[BintrayRepo](
    "Bintray repository.")
}

object InternalBintrayKeys extends InternalBintrayKeys {}
