package bintray

import bintry.Client
import java.io.File
import java.net.URL
import org.apache.ivy.core.module.descriptor.Artifact
import org.apache.ivy.plugins.resolver.IBiblioResolver
import org.apache.ivy.plugins.resolver.URLResolver
import org.apache.ivy.plugins.repository.{ AbstractRepository, Repository }
import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration

case class BintrayMavenRepository(
  underlying: Repository, bty: Client#Repo#Package)
  extends AbstractRepository with DispatchHandlers {

  override def put(artifact: Artifact, src: File, dest: String, overwrite: Boolean): Unit = {
    val destPath = transform(dest)
    Await.result(
      bty.mvnUpload(destPath, src).publish(true)(asStatusAndBody),
      Duration.Inf) match {
        case (201, _) =>
        case (_, fail) =>
          println(fail)
          throw new RuntimeException(s"error uploading to $dest: $fail")
      }
  }

  def getResource(src: String) = underlying.getResource(src)

  def get(src: String, dest: File) = underlying.get(src, dest)

  def list(parent: String) = underlying.list(parent)

  /** transforms a full url like
   *  https://api.bintray.com/maven/:subject/maven/:name/me/lessis/:name_2.10/0.1.0/:name_2.10-0.1.0.pom
   *  into a path like
   *  me/lessis/:name_2.10/0.1.0/:name_2.10-0.1.0.pom
   */
  private def transform(dest: String) =
    new URL(dest).getPath.split('/').drop(5).mkString("/")
}

case class BintrayIvyRepository(
  underlying: Repository,
  bty: Client#Repo#Package#Version)
  extends AbstractRepository with DispatchHandlers {

  override def put(
    artifact: Artifact, src: File, dest: String, overwrite: Boolean): Unit = {
    Await.result(
      bty.upload(dest, src).publish(true)(asStatusAndBody),
      Duration.Inf) match {
        case (201, _) =>
        case (_, fail) =>
          println(fail)
          throw new RuntimeException(s"error uploading to $dest: $fail")
      }
  }

  def getResource(src: String) = underlying.getResource(src)

  def get(src: String, dest: File) = underlying.get(src, dest)

  def list(parent: String) = underlying.list(parent)
}

case class BintrayIvyResolver(
  name: String,
  bty: Client#Repo#Package#Version,
  patterns: Seq[String])
  extends URLResolver {
  import collection.JavaConverters._
  setName(name)
  setM2compatible(false)
  setArtifactPatterns(patterns.toList.asJava)

  override def setRepository(repository: Repository): Unit =
    super.setRepository(BintrayIvyRepository(repository, bty))
}

case class BintrayMavenResolver(
  name: String, rootURL: String, bty: Client#Repo#Package)
  extends IBiblioResolver {
  setName(name)
  setM2compatible(true)
  setRoot(rootURL)
  override def setRepository(repository: Repository): Unit =
    super.setRepository(BintrayMavenRepository(repository, bty))
}
