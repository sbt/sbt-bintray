package bintray

import sbt.{ MavenRepository, RawRepository, Resolver }
import bintry.Client

object Opts {
  object resolver {
    @deprecated("use sbt.Resolver.jcenterRepo instead (available in sbt 0.13.6+)", since = "0.2.0")
    val jcenter = MavenRepository("BintrayJCenter", "https://jcenter.bintray.com")

    def publishTo(repo: Client#Repo, pkg: Client#Repo#Package, version: String, mvnStyle: Boolean = true, isSbtPlugin: Boolean = false) =
      if (mvnStyle) new RawRepository(
        BintrayMavenResolver(s"Bintray-Maven-Publish-${repo.subject}-${repo.repo}-${pkg.name}",
                             s"https://api.bintray.com/maven/${repo.subject}/${repo.repo}/${repo.repo}", pkg))
      else new RawRepository(
        BintrayIvyResolver(s"Bintray-${if (isSbtPlugin) "Sbt" else "Ivy"}-Publish-${repo.subject}-${repo.repo}-${pkg.name}",
                           pkg.version(version),
                           sbt.Resolver.ivyStylePatterns.artifactPatterns))

    @deprecated("""use sbt.Resolver.bintrayRepo(owner,"maven") instead (available in sbt 0.13.6+)""", since = "0.2.0")
    def mavenRepo(name: String) = repo(name, "maven")

    @deprecated("use sbt.Resolver.bintrayRepo(owner,repo) instead (available in sbt 0.13.6+)", since = "0.2.0")
    def repo(name: String, repo: String) =
      MavenRepository(
        s"Bintray-Resolve-$name-$repo",
        s"https://dl.bintray.com/content/$name/$repo")
  }
}
