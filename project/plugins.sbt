resolvers += Resolver.url(
  "bintray-sbt-plugin-releases",
    url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(
        Resolver.ivyStylePatterns)

addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.3")

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.2.1")

resolvers += Resolver.sonatypeRepo("snapshots")
