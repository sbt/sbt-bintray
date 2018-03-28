// skip root project
skip in publish := true

lazy val module = (project in file("module"))
  // skip submodule
  .settings(skip in publish := true)
