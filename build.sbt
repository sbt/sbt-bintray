organization := "me.lessis"

name := "bintray-sbt"

version := "0.1.2-SNAPSHOT"

description := "package publisher for bintray.com"

sbtPlugin := true

resolvers += "softprops-maven" at "http://dl.bintray.com/content/softprops/maven"

libraryDependencies ++= Seq(
  "me.lessis" %% "bintry" % "0.3.0",
  "org.slf4j" % "slf4j-nop" % "1.7.7") // https://github.com/softprops/bintray-sbt/issues/26

scalacOptions ++= Seq(Opts.compile.deprecation, "-feature")

resolvers += Resolver.sonatypeRepo("releases")

licenses ++= Seq("MIT" -> url(
  s"https://github.com/softprops/${name.value}/blob/${version.value}/LICENSE"))

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

seq(bintraySettings:_*)

crossBuildingSettings
