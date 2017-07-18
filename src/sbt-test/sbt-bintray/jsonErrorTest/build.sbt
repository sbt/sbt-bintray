val jsonStr = taskKey[String]("")

// Reproduces issue: https://github.com/sbt/sbt-bintray/issues/104
jsonStr := {
  import org.json4s.JsonDSL._
  import org.json4s.native.JsonMethods

  val jsonMethods = new JsonMethods {}
  jsonMethods.compact(
    jsonMethods.render(
      ("name"     -> "randomName") ~
      ("desc"     -> Option("basicDescription")) ~
      ("licenses" -> List("Apache", "MIT")) ~
      ("labels"   -> List("label1", "label2")) ~
      ("vcs_url"  -> Option("vcs"))
    )
  )
}

TaskKey[Unit]("check") := {
  val expected =
    "{" +
      """"name":"randomName",""" +
      """"desc":"basicDescription",""" +
      """"licenses":["Apache","MIT"],""" +
      """"labels":["label1","label2"],""" +
      """"vcs_url":"vcs"""" +
    "}"
  val found = jsonStr.value
  assert(expected == found, s"Expected: $expected but found: $found")
}
