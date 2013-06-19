package bintray

import sbt.{ MavenRepository, RawRepository }
import sbt._
import bintry._

object Opts {
  object resolver {
    val jcenter = MavenRepository("BintrayJCenter", "http://jcenter.bintray.com")
    def publishTo(name: String, repo: String, pkg: String, bty: Client#Repo#Package) =
      new RawRepository(
        BintrayResolver("Bintray-Publish-%s-%s-%s" format(name, repo, pkg),
        "https://api.bintray.com/maven/%s/%s/%s".format(
          name, repo, pkg), bty))
    def repo(name: String, repo: String) =
      MavenRepository(
        "Bintray-Resolve-%s-%s" format(name, repo),
        "http://dl.bintray.com/content/%s/%s".format(
          name, repo))
  }
}
