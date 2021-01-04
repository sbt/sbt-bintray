package sbtpackages

import sbt._
import org.apache.ivy.plugins.resolver.DependencyResolver

object RawRepository {
  def apply(resolver: DependencyResolver): RawRepository =
    new RawRepository(resolver)
}
