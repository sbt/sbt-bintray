organization := "me.lessis"

name := "bintray-sbt"

version := "0.1.1-SNAPSHOT"

description := "package publisher for bintray.com"

sbtPlugin := true

sbtVersion in Global := "0.13.0-RC2"

scalaVersion in Global := "2.10.2"

libraryDependencies += "me.lessis" %% "bintry" % "0.2.0"

scalacOptions ++= Seq(Opts.compile.deprecation)

resolvers += Resolver.sonatypeRepo("releases")

licenses <++= (version)(v => Seq("MIT" -> url(
  "https://github.com/softprops/bintray-sbt/blob/%s/LICENSE".format(v))))

publishTo := Some(Classpaths.sbtPluginReleases)

publishMavenStyle := false

publishArtifact in Test := false

pomExtra := (
  <scm>
    <url>git@github.com:softprops/bintray-sbt.git</url>
    <connection>scm:git:git@github.com:softprops/bintray-sbt.git</connection>
  </scm>
  <developers>
    <developer>
      <id>softprops</id>
      <name>Doug Tangren</name>
      <url>https://github.com/softprops</url>
    </developer>
  </developers>
)

seq(lsSettings:_*)


