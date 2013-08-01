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
    val destPath = new URL(dest).getPath.split('/').drop(5).mkString("/")
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
}


case class BintraySbtPluginRepository(
  underlying: Repository, btr: Client#Repo, bty: Client#Repo#Package, version: String)
  extends AbstractRepository {
  override def put(artifact: Artifact, src: File, dest: String, overwrite: Boolean): Unit = {
    // subject / repo / package name / version
    // note: btr already takes care of subject/repo prefix
   val totalPath = s"${bty.name}/$version/$dest"
   throw new RuntimeException(s"I want to publish here: $totalPath")
   
   
   /* val (code, body) = btr.upload(totalPath, src, publish = true)(
      new FunctionHandler({ r => (r.getStatusCode, r.getResponseBody) }))()
    if (code != 201) {
      println(body)
      throw new RuntimeException("error uploading to %s: %s" format(dest, body))
    }*/
  }
  def getResource(src: String) = underlying.getResource(src)
  def get(src: String, dest: File) = underlying.get(src, dest)
  def list(parent: String) = underlying.list(parent)
}

case class BintraySbtPluginResolver(
  name: String, repo: Client#Repo, bty: Client#Repo#Package, version: String)
  extends URLResolver {
  setName(name)
  import collection.JavaConverters._
  setM2compatible(false)
  setArtifactPatterns(sbt.Resolver.ivyStylePatterns.artifactPatterns.toList.asJava)
  
  override def setRepository(repository: Repository): Unit =
    super.setRepository(BintraySbtPluginRepository(repository, repo, bty, version: String))
}

case class BintrayResolver(
  name: String, url: String, bty: Client#Repo#Package)
  extends IBiblioResolver {

  setName(name)
  setM2compatible(true)
  setRoot(url)

  override def setRepository(repository: Repository): Unit =
    super.setRepository(BintrayMavenRepository(repository, bty))
}
