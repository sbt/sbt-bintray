
lazy val mavenStyle = (project in file("maven-style"))
  .settings(
    version := "0.1",
    scalaVersion := "2.10.6",
    publishMavenStyle := true,
    bintrayOrganization := Some("tinyrick"),
    bintrayRepository := "evilmorty",
    TaskKey[Unit]("check") := {
      assert(resolvers.value.filter(_.name.equals("bintray-tinyrick-evilmorty")).head.isInstanceOf[MavenRepository],
        "A maven style project should have a maven repository as it default resolver"
      )
    }
  )

lazy val ivyStyle = (project in file("ivy-style"))
  .settings(
    version := "0.1",
    scalaVersion := "2.10.6",
    publishMavenStyle := false,
    bintrayOrganization := Some("tinyrick"),
    bintrayRepository := "evilmorty",
    TaskKey[Unit]("check") := {
      assert(resolvers.value.filter(_.name.equals("bintray-tinyrick-evilmorty")).head.isInstanceOf[URLRepository],
        "An ivy style project should have a URL repository as it default resolver"
      )
    }
  )