resolvers += Resolver.url(
  "bintray-sbt-plugin-releases",
    url("http://dl.bintray.com/content/sbt/sbt-plugin-releases"))(
        Resolver.ivyStylePatterns)

// addSbtPlugin("me.lessis" % "ls-sbt" % "0.1.3")

addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")
addSbtPlugin("com.dwijnand" % "sbt-dynver" % "1.3.0")

libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value
