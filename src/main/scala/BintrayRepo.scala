package bintray

import sbt._
import Bintray._
import bintry.{ Attr, Client, Licenses }

case class BintrayRepo(credential: BintrayCredentials, org: Option[String], repoName: String) extends DispatchHandlers {
  import scala.concurrent.ExecutionContext.Implicits.global
  import dispatch.as

  lazy val BintrayCredentials(user, key) = credential
  def client: Client = Client(user, key)
  def repo: Client#Repo = client.repo(org.getOrElse(user), repoName)
  def owner = org.getOrElse(user)

  /** updates a package version with the values defined in versionAttributes in bintray */
  def publishVersionAttributes(packageName: String, vers: String, attributes: AttrMap): Unit =
    await.ready {
      repo.get(packageName).version(vers).attrs.update(attributes.toList:_*)()
    }

  /** Ensure user-specific bintray package exists. This will have a side effect of updating the packages attrs
   *  when it exists.
   *  todo(doug): Perhaps we want to factor that into an explicit task. */
  def ensurePackage(packageName: String, attributes: AttrMap,
    desc: String, vcs: String, lics: Seq[(String, URL)], labels: Seq[String]): Unit =
    {
      val bty = repo
      val exists =
        if (await.result(bty.get(packageName)(asFound))) {
          // update existing attrs
          if (!attributes.isEmpty) await.ready(bty.get(packageName).attrs.update(attributes.toList:_*)())
          true
        } else {
          val created = await.result(
            bty.createPackage(packageName)
              .desc(desc)
              .vcs(vcs)
              .licenses(lics.map { case (name, _) => name }:_*)
              .labels(labels:_*)(asCreated))
          // assign attrs
          if (created && !attributes.isEmpty) await.ready(
            bty.get(packageName).attrs.set(attributes.toList:_*)())
          created
        }
      if (!exists) sys.error(
        s"was not able to find or create a package for $owner in $repo named $packageName")
    }

  def buildPublishResolver(packageName: String, vers: String, mvnStyle: Boolean, isSbtPlugin: Boolean): Resolver =
    {
      val bty = repo
      val pkg = bty.get(packageName)
      // warn the user that bintray expects maven published artifacts to be published to the `maven` repo
      // but they have explicitly opted into a publish style and/or repo that
      // deviates from that expecation
      if ("maven" == repo && !mvnStyle) println(
        "you have opted to publish to a repository named 'maven' but publishMavenStyle is assigned to false. This may result in unexpected behavior")
      Opts.resolver.publishTo(bty, pkg, vers, mvnStyle, isSbtPlugin)
    }

  /** unpublish (delete) a version of a package */
  def unpublish(packageName: String, vers: String, log: Logger): Unit =
    await.result(repo.get(packageName).version(vers).delete(asStatusAndBody)) match {
      case (200, _) =>  log.info(s"$owner/$packageName@$vers was discarded")
      case (_, fail) => sys.error(s"failed to discard $owner/$packageName@$vers: $fail")
    }

  /** pgp sign remotely published artifacts then publish those signed artifacts.
   *  this assumes artifacts are published remotely. signing artifacts doesn't
   *  mean the signings themselves will be published so it is nessessary to publish
   *  this immediately after.
   */
  def remoteSign(packageName: String, vers: String, log: Logger): Unit =
    {
      val bty = repo
      val btyVersion = bty.get(packageName).version(vers)
      val passphrase = Cache.get("pgp.pass").orElse(Prompt.descretely("Enter pgp passphrase"))
        .getOrElse {
          sys.error("pgp passphrase is required")
        }
      val (status, body) = await.result(
        btyVersion.sign(passphrase)(asStatusAndBody))
      if (status == 200) {
        // we want to only ask for pgp credentials once for a given sbt session
        // so let's cache them for later use in the session after we're reasonable
        // sure they are valid
        Cache.put(("pgp.pass", passphrase))
        log.info(s"$owner/$packageName@$vers was signed")
        // after signing the remote artifacts, they remain
        // unpublished (not available for download)
        // we are opting to publish those unpublished
        // artifacts here
        val (pubStatus, pubBody) = await.result(
          btyVersion.publish(asStatusAndBody))
        if (pubStatus != 200) sys.error(
          s"failed to publish signed artifacts for $owner/$packageName@$vers: $pubBody")
      }
      else sys.error(s"failed to sign $owner/$packageName@$vers: $body")
    }

  /** synchronize a published set of artifacts for a pkg version to mvn central
   *  this requires already having a sonatype oss account set up.
   *  this is itself quite a task but in the case the user has done this in the past
   *  this can be quiet a convenient feature */
  def syncMavenCentral(packageName: String, vers: String, creds: Seq[Credentials], log: Logger): Unit =
    {
      val bty = repo
      val btyVersion = bty.get(packageName).version(vers)
      val BintrayCredentials(sonauser, sonapass) =
        resolveSonatypeCredentials(creds)
      await.result(
        btyVersion.mavenCentralSync(sonauser, sonapass)(asStatusAndBody)) match {
        case (200, body) =>
          // store these sonatype credentials in memory for the remainder of the sbt session
          Cache.putMulti(
            ("sona.user", sonauser), ("sona.pass", sonapass))
          log.info(s"$owner/$packageName@$vers was synced with maven central")
          log.info(body)
        case (404, body) =>
          log.info(s"$owner/$packageName@$vers was not found. try publishing this package version to bintray first by typing `publish`")
          log.info(s"body $body")
        case (_, body) =>
          // ensure these items are removed from the cache, they are probably bad
          Cache.removeMulti("sona.user", "sona.pass")
          sys.error(s"failed to sync $owner/$packageName@$vers with maven central: $body")
        }
    }

  private def resolveSonatypeCredentials(
    creds: Seq[sbt.Credentials]): BintrayCredentials =
    Credentials.forHost(creds, BintrayCredentials.sonatype.Host)
      .map { d => (d.userName, d.passwd) }
      .getOrElse(requestSonatypeCredentials) match {
        case (user, pass) => BintrayCredentials(user, pass)
      }

  /** Search Sonatype credentials in the following order:
   *  1. Cache
   *  2. System properties
   *  3. Environment variables
   *  4. User input */
  private def requestSonatypeCredentials: (String, String) = {
    val cached = Cache.getMulti("sona.user", "sona.pass")
    (cached("sona.user"), cached("sona.pass")) match {
      case (Some(user), Some(pass)) =>
        (user, pass)
      case _ =>
        val propsCredentials = for (name <- sys.props.get("sona.user"); pass <- sys.props.get("sona.pass")) yield (name, pass)
        propsCredentials match {
          case Some((name, pass)) => (name, pass)
          case _ =>
            val envCredentials = for (name <- sys.env.get("SONA_USER"); pass <- sys.env.get("SONA_PASS")) yield (name, pass)
            envCredentials.getOrElse {
              val name = Prompt("Enter sonatype username").getOrElse {
                sys.error("sonatype username required")
              }
              val pass = Prompt.descretely("Enter sonatype password").getOrElse {
                sys.error("sonatype password is required")
              }
              (name, pass)
            }
        }
    }
  }

  /** Lists versions of bintray packages corresponding to the current project */
  def packageVersions(packageName: String, log: Logger): Seq[String] =
    {
      import _root_.org.json4s._
      val pkg = repo.get(packageName)
      log.info(s"fetching package versions for package $packageName")
      await.result(pkg(EitherHttp({ _ => JNothing}, as.json4s.Json))).fold({ js =>
        log.warn("package does not exist")
        Nil
      }, { js =>
        for {
          JObject(fs)                    <- js
          ("versions", JArray(versions)) <- fs
          JString(versionString)         <- versions
        } yield {
          log.info(s"- $versionString")
          versionString
        }
      })
    }
}
