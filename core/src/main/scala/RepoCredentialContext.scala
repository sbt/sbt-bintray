package sbtpackages

import java.io.File

/**
 * This abstract which environmental variable will be used by which plugin.
 */
final case class BintrayCredentialContext(
  credsFile: File,
  userNameProp: String,
  passProp: String,
  userNameEnv: String,
  passEnv: String
) extends RepoCredentialContext[BintrayCredentialContext]

object BintrayCredentialContext extends RepoCredentialContextObj[BintrayCredentialContext] {
  protected def propertiesPrefix = "bintray"
  protected def environmentPrefix = "BINTRAY"

  def apply(
    credsFile: File,
    userNameProp: String,
    passProp: String,
    userNameEnv: String,
    passEnv: String
  ): BintrayCredentialContext = BintrayCredentialContext(credsFile, userNameProp, passProp, userNameEnv, passEnv)
}

final case class GitHubCredentialContext(
  credsFile: File,
  userNameProp: String,
  passProp: String,
  userNameEnv: String,
  passEnv: String
) extends RepoCredentialContext[GitHubCredentialContext]

object GitHubCredentialContext extends RepoCredentialContextObj[GitHubCredentialContext] {
  protected def propertiesPrefix = "github"
  protected def environmentPrefix = "GITHUB"

  def apply(
    credsFile: File,
    userNameProp: String,
    passProp: String,
    userNameEnv: String,
    passEnv: String
  ): GitHubCredentialContext = GitHubCredentialContext(credsFile, userNameProp, passProp, userNameEnv, passEnv)
}

sealed trait RepoCredentialContextObj[A <: RepoCredentialContext[A]] {
  protected def propertiesPrefix: String
  protected def environmentPrefix: String

  final def apply(credsFile: File): A = apply(
    credsFile,
    s"$propertiesPrefix.user",
    s"$propertiesPrefix.pass",
    s"${environmentPrefix}_USER",
    s"${environmentPrefix}_PASS"
  )

  final def remoteCache(credsFile: File): A = apply(
    credsFile,
    s"$propertiesPrefix.remote.cache.user",
    s"$propertiesPrefix.remote.cache.pass",
    s"${environmentPrefix}_REMOTE_CACHE_USER",
    s"${environmentPrefix}_REMOTE_CACHE_PASS"
  )

  def apply(
   credsFile: File,
   userNameProp: String,
   passProp: String,
   userNameEnv: String,
   passEnv: String
  ): A
}

sealed trait RepoCredentialContext[A <: RepoCredentialContext[A]] {
  def credsFile: File
  def userNameProp: String
  def passProp: String
  def userNameEnv: String
  def passEnv: String
}