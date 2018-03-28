# sbt-bintray [![travis-badge][]][travis] [![gitter-badge][]][gitter]

[travis]:                https://travis-ci.org/sbt/sbt-bintray
[travis-badge]:          https://travis-ci.org/sbt/sbt-bintray.svg?branch=master
[gitter]:                    https://gitter.im/sbt/sbt-bintray
[gitter-badge]:       https://badges.gitter.im/sbt/sbt-bintray.svg


An sbt plugin for publishing [bintray](https://bintray.com) packages.

## Consuming or publishing?

This plugin is for publishing.  You don't need this plugin to consume
Bintray artifacts; sbt supports that directly.  If the library you want
is published to [JCenter](https://bintray.com/bintray/jcenter) (which is
like Maven Central, but for Bintray), just add:

```scala
resolvers += Resolver.jcenterRepo
```

Some libraries are published to a particular user's Bintray repo.
In that case, do:

```scala
resolvers += Resolver.bintrayRepo("otherUser", "maven")
```

The repo name is typically `"maven"` but might be something else.

If you want to _publish_ to Bintray, read on.

## Install

### What you need

- sbt 0.13.16 or newer
- an account on [bintray](https://bintray.com) (get one [here](https://bintray.com/signup/index))
- a desire to build a more diverse Scala library ecosystem

Add the following to your sbt `project/plugins.sbt` file:

```scala
addSbtPlugin("org.foundweekends" % "sbt-bintray" % "0.5.3")
```

## Usage

Note that when specifying `sbt-bintray` settings in `project/*.scala` files (as opposed to in `build.sbt`), you will need to add the following import:

```scala
import bintray.BintrayKeys._
```

### Publishing

To publish a package to bintray, you need a bintray account. You can register for one [here](https://bintray.com/signup/index). 
`BintrayPlugin` is an auto plugin that will be added to all projects in your build.
This plugin will upload and release your artifacts into bintray when you run `publish`.

To exclude a project from being published (for example a root or a tests project) use the `skip` setting:

```scala
skip in publish := true
```

At any time you can check who you will be authenticated as with the `bintrayWhoami` setting which will print your bintray username

    > bintrayWhoami

#### Credentials

To publish, you need to provide Bintray credentials (user name and API key). There are three ways to set them up: credential file, properties, and environment variables.

1. Credentials file

sbt-bintray will look for a credentials file under `~/.bintray/.credentials` used to authenticate publishing requests to bintray.

You can interactively set up or change the bintray credentials used by sbt anytime with

    > bintrayChangeCredentials

Note you will need to `reload` your project afterwards which will reset your `publishTo` setting.

2.  Properties

You can pass the user and pass as JVM properties when starting sbt:

    sbt -Dbintray.user=yourBintrayUser -Dbintray.pass=yourBintrayPass
    
3. Environment variables

sbt-bintray will look for bintray user and pass in the environment variables `BINTRAY_USER` and  `BINTRAY_PASS`.

#### Bintray organization

You may optionally wish to publish to a [bintray organization](https://bintray.com/docs/usermanual/interacting/interacting_bintrayorganizations.html)
instead of your individual bintray user account. To do so, use the `bintrayOrganization` setting in your project's build definition.

```scala
bintrayOrganization := Some("strength-in-numbers")
```

By default, a [bintray Maven repository](https://bintray.com/docs/usermanual/uploads/uploads_yourrepositories.html) for a bintray user or
organization is named `maven`.  If your Maven repository is named differently, you will need to specify the `bintrayRepository` setting.

```scala
bintrayRepository := "oss-maven"
```

#### Staging (optional)

If you want to stage your all artifacts first, put this in your settings:

```scala
bintrayReleaseOnPublish in ThisBuild := false
```

This will break the process into two parts:

1. First, stage all artifacts using `publish`.
2. Once all artifacts are staged, run `bintrayRelease` to make the artifacts public

#### Licenses

##### Private

If your project does not use a license, you may opt out of specifying one:

```scala
bintrayOmitLicense := true
```

##### Public (default)

If your project uses a license, Bintray supports several [OSS licenses](https://bintray.com/docs/api/#_get_oss_licenses). If you are new to software licenses you may
want to grab a coffee and absorb some [well organized information](http://choosealicense.com/) on the topic of choice.
Sbt already defines a `licenses` setting key. In order to use bintray sbt you must define your `licenses` key to contain a license with a name matching
one of those bintray defines. I recommend [MIT](http://choosealicense.com/licenses/mit/).

```scala
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
```

#### Labels

The first time you publish a bintray package, this plugin will create the package for you on bintray. Along with the actual contents
of the package, you can list a publicly visible list of labels that related to your package.

You can assign this with the `bintrayPackageLabels` setting key.

```scala
bintrayPackageLabels := Seq("hipster", "keen")
```

#### Metadata

In addition to labels, you can also assign metadata attributes that expose information to package tooling. These can be assigned at the package and the version levels. By default, this plugin assigns a flag indicating "this is an sbt plugin" to the package and the scala version and optionally sbt version to the package version. You can assign these with the `packageAttributes in bintray` and `versionAttributes in bintray` setting keys. These values must be typed and conform to the [types](https://github.com/softprops/bintry#metadata) bintray [exposes](https://bintray.com/docs/api/#_attributes).

```scala
// append custom package attributes
bintrayPackageAttributes ~=
  ((_: bintray.AttrMap) ++ Map("my-package-attr" -> Seq(bintry.StringAttr("my-value"))))
```

```scala
// append custom version attributes
bintrayVersionAttributes ~=
  ((_: bintray.AttrMap) ++ Map("my-version-attr" -> Seq(bintry.BooleanAttr(true))))
```

_NOTE_ This interface will likely change in the future. All changes will be announced and well documented.

##### Other pieces of flair

When publishing for the first time, bintray sbt will create a package for you under your bintray account's "maven" repository
with your project's (module)name as the package name and description for your package description.

### Unpublishing

It's generally a bad practice to remove a version of a library others may depend on but sometimes you may want test a release with the ability to immediately take it back down if something goes south before others start depending on it. Bintray allows for this flexibility and thus, sbt-bintray does as well. Use the `unpublish` task to unpublish the current version from bintray.

    > bintrayUnpublish

### Finding your way around

The easiest way to learn about sbt-bintray is to use the sbt shell typing `bintray<tab>` or `help bintray` to discover bintray keys.

## Acknowledgments

This plugin was first created by Doug Tangren (softprops), 2013-2014.

The plugin is now community-maintained. Releases are published by the sbt team at Lightbend.
