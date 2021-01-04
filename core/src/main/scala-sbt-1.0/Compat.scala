package sbtpackages

import scala.language.reflectiveCalls

import sbt.librarymanagement.{ RawRepository => SbtRawRepository }

object RawRepository {
  def apply(resolver: AnyRef): SbtRawRepository =
    new SbtRawRepository(resolver, resolver.asInstanceOf[{ def getName(): String }].getName)
}
