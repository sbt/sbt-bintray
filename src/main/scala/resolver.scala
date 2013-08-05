package bintray

import java.io.File
import java.net.URL
import org.apache.ivy.core.module.descriptor.Artifact
import org.apache.ivy.plugins.resolver.IBiblioResolver
import org.apache.ivy.plugins.resolver.URLResolver
import org.apache.ivy.plugins.repository.{ AbstractRepository, Repository, TransferEvent }
import org.apache.ivy.plugins.repository.url.URLResource
import bintry._
import dispatch._
import sbt.PatternsBasedRepository

case class BintrayMavenRepository(
  underlying: Repository, bty: Client#Repo#Package)
  extends AbstractRepository {

  override def put(artifact: Artifact, src: File, dest: String, overwrite: Boolean): Unit = {
    val destPath = transform(dest)
    val (code, body) = bty.mvnUpload(destPath, src, publish = true)(
      new FunctionHandler({ r => (r.getStatusCode, r.getResponseBody) }))()
    if (code != 201) {
      println(body)
      throw new RuntimeException("error uploading to %s: %s" format(dest, body))
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

case class BintraySbtPluginRepository(
  underlying: Repository, bty: Client#Repo#Package#Version)
  extends AbstractRepository {

  override def put(artifact: Artifact, src: File, dest: String, overwrite: Boolean): Unit = {
    val (code, body) = bty.upload(dest, src, publish = true)(
      new FunctionHandler({ r => (r.getStatusCode, r.getResponseBody) }))()
    if (code != 201) {
      println(body)
      throw new RuntimeException("error uploading to %s: %s" format(dest, body))
    }
  }

  def getResource(src: String) = underlying.getResource(src)

  def get(src: String, dest: File) = underlying.get(src, dest)

  def list(parent: String) = underlying.list(parent)
}

case class BintraySbtPluginResolver(
  name: String, bty: Client#Repo#Package#Version)
  extends URLResolver {
  import collection.JavaConverters._
  setName(name)
  setM2compatible(false)
  setArtifactPatterns(sbt.Resolver.ivyStylePatterns.artifactPatterns.toList.asJava)
  override def setRepository(repository: Repository): Unit =
    super.setRepository(BintraySbtPluginRepository(repository, bty))
}

case class BintrayResolver(
  name: String, rootURL: String, bty: Client#Repo#Package)
  extends IBiblioResolver {
  setName(name)
  setM2compatible(true)
  setRoot(rootURL)
  override def setRepository(repository: Repository): Unit =
    super.setRepository(BintrayMavenRepository(repository, bty))
}
