addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.3")
addSbtPlugin("com.dwijnand"      % "sbt-dynver"  % "3.3.0")

libraryDependencies += "org.scala-sbt" % "scripted-plugin" % sbtVersion.value
