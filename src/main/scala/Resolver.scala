package bintray

import bintry.Client
import java.io.File
import java.net.URL
import org.apache.ivy.core.module.descriptor.Artifact
import org.apache.ivy.plugins.resolver.IBiblioResolver
import org.apache.ivy.plugins.resolver.URLResolver
import org.apache.ivy.plugins.repository.{ AbstractRepository, Repository }
import scala.concurrent.Await
import scala.concurrent.duration.Duration

case class BintrayMavenRepository(
  underlying: Repository,
  bty: Client#Repo#Package,
  release: Boolean)
  extends AbstractRepository with DispatchHandlers {

  override def put(artifact: Artifact, src: File, dest: String, overwrite: Boolean): Unit =
    Await.result(
      bty.mvnUpload(dest, src).publish(release)(asStatusAndBody),
      Duration.Inf) match {
        case (201, _) =>
        case (_, fail) =>
          throw new RuntimeException(s"error uploading to $dest: $fail")
      }

  def getResource(src: String) = underlying.getResource(src)

  def get(src: String, dest: File) = underlying.get(src, dest)

  def list(parent: String) = underlying.list(parent)
}

case class BintrayIvyRepository(
  underlying: Repository,
  bty: Client#Repo#Package#Version,
  release: Boolean)
  extends AbstractRepository with DispatchHandlers {

  override def put(
    artifact: Artifact, src: File, dest: String, overwrite: Boolean): Unit =
    Await.result(
      bty.upload(dest, src).publish(release)(asStatusAndBody),
      Duration.Inf) match {
        case (201, _) =>
        case (_, fail) =>
          throw new RuntimeException(s"error uploading to $dest: $fail")
      }

  def getResource(src: String) = underlying.getResource(src)

  def get(src: String, dest: File) = underlying.get(src, dest)

  def list(parent: String) = underlying.list(parent)
}

import collection.JavaConverters.seqAsJavaListConverter
case class BintrayIvyResolver(
  name: String,
  bty: Client#Repo#Package#Version,
  patterns: Seq[String],
  release: Boolean)
  extends URLResolver {
  setName(name)
  setM2compatible(false)
  setArtifactPatterns(patterns.toList.asJava)

  override def setRepository(repository: Repository): Unit =
    super.setRepository(BintrayIvyRepository(repository, bty, release))
}

case class BintrayMavenResolver(
  name: String,
  rootURL: String,
  bty: Client#Repo#Package,
  patterns: Seq[String],
  release: Boolean)
  extends IBiblioResolver {
  setName(name)
  setM2compatible(true)
  setRoot(rootURL)
  setArtifactPatterns(patterns.toList.asJava)
  override def setRepository(repository: Repository): Unit =
    super.setRepository(BintrayMavenRepository(repository, bty, release))
}
