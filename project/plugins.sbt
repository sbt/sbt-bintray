addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.6")
addSbtPlugin("com.dwijnand"      % "sbt-dynver"  % "4.1.0")

libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value
