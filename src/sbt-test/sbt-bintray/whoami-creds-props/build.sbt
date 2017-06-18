TaskKey[Unit]("check") := {
  val whoami = bintrayWhoami.value
  if (whoami != "username") sys.error(s"unexpected whoami output: $whoami")
  ()
}
