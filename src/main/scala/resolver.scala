package bintray

import java.io.File
import java.net.URL
import org.apache.ivy.core.module.descriptor.Artifact
import org.apache.ivy.plugins.resolver.IBiblioResolver
import org.apache.ivy.plugins.repository.{ AbstractRepository, Repository, TransferEvent }
import org.apache.ivy.plugins.repository.url.URLResource
import bintry._
import dispatch._

case class BintrayRepository(
  underlying: Repository, bty: Client#Repo#Package)
  extends AbstractRepository {
  def getResource(src: String) = underlying.getResource(src)
  def get(src: String, dest: File) = underlying.get(src, dest)
  override def put(artifact: Artifact, src: File, dest: String, overwrite: Boolean) {
    println("repo put artifact %s src %s dest %s..." format(artifact, src, dest))
    val destPath = new URL(dest).getPath.split('/').drop(4).mkString("/")
    val (code, body) = bty.mvnUpload(destPath, src, publish = true)(
      new FunctionHandler({ r => (r.getStatusCode, r.getResponseBody) }))()
    if (code != 201) {
      println(body)
      throw new RuntimeException("error uploading to %s: %s" format(dest, body))
    }
  }

  def list(parent: String) = underlying.list(parent)
}

case class BintrayResolver(
  name: String, url: String, bty: Client#Repo#Package)
  extends IBiblioResolver {

  setName(name)
  setM2compatible(true)
  setRoot(url)

  override def setRepository(repository: Repository) = {
    println("setting repository wrapper for %s" format repository)
    super.setRepository(BintrayRepository(repository, bty))
  }
}
