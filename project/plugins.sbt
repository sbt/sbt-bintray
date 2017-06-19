resolvers += Resolver.url(
  "bintray-sbt-plugin-releases",
    url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(
        Resolver.ivyStylePatterns)
resolvers += Resolver.sonatypeRepo("snapshots")

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")
libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value