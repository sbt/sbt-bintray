package bintray

import java.io.File

/**
 * This abstract which environmental variable will be used by which plugin.
 */
case class BintrayCredentialContext(
  credsFile: File,
  userNameProp: String,
  passProp: String,
  userNameEnv: String,
  passEnv: String
)

object BintrayCredentialContext {
  def apply(credsFile: File): BintrayCredentialContext =
    BintrayCredentialContext(
      credsFile,
      "bintray.user",
      "bintray.pass",
      "BINTRAY_USER",
      "BINTRAY_PASS"
    )

  def remoteCache(credsFile: File): BintrayCredentialContext =
    BintrayCredentialContext(
      credsFile,
      "bintray.remote.cache.user",
      "bintray.remote.cache.pass",
      "BINTRAY_REMOTE_CACHE_USER",
      "BINTRAY_REMOTE_CACHE_PASS"
    )
}
