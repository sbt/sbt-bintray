organization := "me.lessis"

name := "bintray-sbt"

version := "0.1.2-SNAPSHOT"

description := "package publisher for bintray.com"

sbtPlugin := true

libraryDependencies += "me.lessis" %% "bintry" % "0.2.0"

scalacOptions ++= Seq(Opts.compile.deprecation, "-feature")

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


seq(bintraySettings:_*)

crossBuildingSettings
