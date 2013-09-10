package bintray

import sbt._

object Def {
  type Initialize[T] = Project.Initialize[T]
}
