package bintray

import sbt.{ MavenRepository, RawRepository }
import sbt._
import bintry._

object Opts {
  object resolver {
    val jcenter = MavenRepository("BintrayJCenter", "http://jcenter.bintray.com")

    def publishTo(repo: Client#Repo, pkg: Client#Repo#Package, version: String, isSbtPlugin: Boolean = false) = 
      if (isSbtPlugin) {
        new RawRepository(
          BintraySbtPluginResolver("Bintray-Sbt-Publish-%s-%s-%s" format(repo.sub, repo.repo, pkg.name), 
             pkg.version(version)))
      } else {
        new RawRepository(
          BintrayResolver("Bintray-Maven-Publish-%s-%s-%s" format(repo.sub, repo.repo, pkg.name),
          "https://api.bintray.com/maven/%s/%s/%s".format(
            repo.sub, repo.repo, pkg.name), pkg))
      }

    def repo(name: String, repo: String) =
      MavenRepository(
        "Bintray-Resolve-%s-%s" format(name, repo),
        "http://dl.bintray.com/content/%s/%s".format(
          name, repo))
  }
}
