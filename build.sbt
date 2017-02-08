lazy val core = project in file("spg.core")

lazy val cmd = (project in file("spg.cmd"))
  .dependsOn(core)
