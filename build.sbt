organization := "me.lessis"

name := "bintray-sbt"

version := "0.1.2-patch"

description := "package publisher for bintray.com"

homepage := Some(url(s"https://github.com/softprops/${name.value}#readme"))

sbtPlugin := true

libraryDependencies ++= Seq(
  "me.lessis" %% "bintry" % "0.4.0-SNAPSHOT",
  "org.slf4j" % "slf4j-nop" % "1.7.7") // https://github.com/softprops/bintray-sbt/issues/26

scalacOptions ++= Seq(Opts.compile.deprecation, "-feature")

resolvers += Resolver.sonatypeRepo("releases")

licenses ++= Seq("MIT" -> url(
  s"https://github.com/softprops/${name.value}/blob/${version.value}/LICENSE"))

publishArtifact in Test := false

pomExtra := (
  <scm>
    <url>git@github.com:softprops/{name.value}.git</url>
    <connection>scm:git:git@github.com:softprops/{name.value}.git</connection>
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

externalResolvers in LsKeys.lsync := (resolvers in bintray.Keys.bintray).value

seq(bintraySettings:_*)

crossBuildingSettings
