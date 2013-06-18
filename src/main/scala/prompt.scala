package bintray

import sbt.SimpleReader

object Prompt {
  def opt(s: String) = s.trim match {
    case e if (e.isEmpty) => None
    case s => Some(s)
  }

  def apply[T](msg: String): Option[String] =
    opt(SimpleReader.readLine("%s: " format msg).get)

  def descretely[T](msg: String): Option[String] =
    opt(SimpleReader.readLine("%s: " format msg, Some('*')).get)
}
