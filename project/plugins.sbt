resolvers += Resolver.url(
  "bintray-sbt-plugin-releases",
    url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(
        Resolver.ivyStylePatterns)

addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.3")

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.2.1-SNAPSHOT")

resolvers += Resolver.sonatypeRepo("snapshots")

addSbtPlugin("net.virtual-void" % "sbt-cross-building" % "0.8.1-SNAPSHOT")
