package bintray

import org.json4s.JValue

object JsonImplicits {
  // 2.10 withFilter support (submitted a patch upstream for the next rel)
  implicit class LiftJValueWithFilter(self: JValue)
         extends JValueWithFilter(self, _ => true)
  class JValueWithFilter(self: JValue, p: JValue => Boolean) {
    def map[T](f: JValue => T): List[T] =
      self.filter(p).map(f)
    def flatMap[T](f: JValue => List[T]): List[T] =
      self.filter(p).flatMap(f)
    def foreach(f: JValue => Unit): Unit =
      self.filter(p).foreach(f)
    def withFilter(q: JValue => Boolean): JValueWithFilter =
      new JValueWithFilter(self, x => p(x) && q(x))
  }
}
