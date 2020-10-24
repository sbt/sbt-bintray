package bintray

import scala.language.reflectiveCalls

import sbt.librarymanagement.RawRepository

object RawRepository {
  def apply(resolver: AnyRef): RawRepository =
    new RawRepository(resolver, resolver.asInstanceOf[{ def getName(): String }].getName)
}
