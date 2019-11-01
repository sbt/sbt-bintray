lazy val unusedWarnings = Seq("-Ywarn-unused-import", "-Ywarn-unused")

lazy val commonSettings: Seq[Setting[_]] = Seq(
    organization in ThisBuild := "org.foundweekends",
    homepage in ThisBuild := Some(url(s"https://github.com/sbt/${name.value}/#readme")),
    licenses in ThisBuild := Seq("MIT" ->
      url(s"https://github.com/sbt/${name.value}/blob/${version.value}/LICENSE")),
    description in ThisBuild := "package publisher for bintray.com",
    developers in ThisBuild := List(
      Developer("softprops", "Doug Tangren", "@softprops", url("https://github.com/softprops"))
    ),
    scmInfo in ThisBuild := Some(ScmInfo(url(s"https://github.com/sbt/${name.value}"), s"git@github.com:sbt/${name.value}.git")),
    // crossScalaVersions in ThisBuild := Seq("2.10.7", "2.11.8", "2.12.6"),
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
  .settings(commonSettings)
  .settings(
    name := "sbt-bintray",
    sbtPlugin := true,
    crossSbtVersions := List("0.13.18", "1.0.0"),
    scalaVersion := (CrossVersion partialVersion sbtCrossVersion.value match {
      case Some((0, 13)) => "2.10.7"
      case Some((1, _))  => "2.12.6"
      case _             => sys error s"Unhandled sbt version ${sbtCrossVersion.value}"
    }),
    libraryDependencies ++= Seq(
      "org.foundweekends" %% "bintry" % "0.5.2",
      "org.slf4j" % "slf4j-nop" % "1.7.29"), // https://github.com/sbt/sbt-bintray/issues/26
    resolvers += Resolver.sonatypeRepo("releases"),
    scriptedSettings,
    scriptedBufferLog := true,
    scriptedLaunchOpts ++= Seq(
      "-Xmx1024M",
      "-XX:MaxPermSize=256M",
      "-Dbintray.user=username",
      "-Dbintray.pass=password",
      "-Dplugin.version=" + version.value
    )
  )

val sbtCrossVersion = sbtVersion in pluginCrossBuild
