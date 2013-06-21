organization := "me.lessis"

name := "bintray-sbt"

version := "0.1.0-SNAPSHOT" 

sbtPlugin := true

sbtVersion in Global := "0.13.0-Beta2"

scalaVersion in Global := "2.10.2-RC2"

libraryDependencies += "me.lessis" %% "bintry" % "0.1.0"

scalacOptions ++= Seq(Opts.compile.deprecation)
