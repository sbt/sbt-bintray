lazy val unusedWarnings = Seq("-Ywarn-unused-import", "-Ywarn-unused")

lazy val commonSettings: Seq[Setting[_]] = Seq(
    version in ThisBuild := "0.4.0-SNAPSHOT",
    organization in ThisBuild := "org.foundweekends",
    homepage in ThisBuild := Some(url(s"https://github.com/sbt/${name.value}/#readme")),
    licenses in ThisBuild := Seq("MIT" ->
      url(s"https://github.com/sbt/${name.value}/blob/${version.value}/LICENSE")),
    description in ThisBuild := "package publisher for bintray.com",
    developers in ThisBuild := List(
      Developer("softprops", "Doug Tangren", "@softprops", url("https://github.com/softprops"))
    ),
    scmInfo in ThisBuild := Some(ScmInfo(url(s"https://github.com/sbt/${name.value}"), s"git@github.com:sbt/{name.value}.git")),
    // crossScalaVersions in ThisBuild := Seq("2.10.6", "2.11.8", "2.12.2"),
    scalaVersion := (crossScalaVersions in ThisBuild).value.last,
    scalacOptions ++= Seq(Opts.compile.deprecation, "-Xlint", "-feature"),
    scalacOptions ++= PartialFunction.condOpt(CrossVersion.partialVersion(scalaVersion.value)){
      case Some((2, v)) if v >= 11 => unusedWarnings
    }.toList.flatten,
    publishArtifact in Test := false,
    bintrayRepository := "sbt-plugin-releases",
    bintrayOrganization := Some("sbt"),
    bintrayPackage := "sbt-bintray"
  ) ++ Seq(Compile, Test).flatMap(c =>
    scalacOptions in (c, console) --= unusedWarnings
  )

lazy val root = (project in file("."))
  .settings(
    name := "sbt-bintray",
    sbtPlugin := true,
    libraryDependencies ++= Seq(
      "org.foundweekends" %% "bintry" % "0.5.0",
      "org.slf4j" % "slf4j-nop" % "1.7.7"), // https://github.com/softprops/bintray-sbt/issues/26
    resolvers += Resolver.sonatypeRepo("releases")
  )
  // .settings(
  //   scalaVersion := "2.12.2",
  //   sbtVersion in Global := "1.0.0-M5",
  //   scalaCompilerBridgeSource :=
  //     ("org.scala-sbt" % "compiler-interface" % "0.13.15" % "component").sources
  // )
