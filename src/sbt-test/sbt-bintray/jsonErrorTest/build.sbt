enablePlugins(BintrayPlugin)


val testJsonSerialization = inputKey[Unit]("testJsonSerialization")

testJsonSerialization := {
  val expected =
    "{" +
      """"name":"randomName",""" +
      """"desc":"basicDescription",""" +
      """"licenses":["Apache","MIT"],""" +
      """"labels":["label1","label2"],""" +
      """"vcs_url":"vcs"""" +
    "}"
  val found: String = __bintrayErrorRepro.value
  assert(expected == found, s"Expected: $expected but found: $found")
}