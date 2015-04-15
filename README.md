# bintray sbt

An sbt interface for publishing and resolving [bintray](https://bintray.com) packages.

## install

### what you need

- an account on [bintray](https://bintray.com) (get one [here](https://bintray.com/signup/index))
- a desire to build a more diverse Scala library ecosystem

Add the following to your sbt `project/plugins.sbt` file:

```scala
addSbtPlugin("me.lessis" % "bintray-sbt" % "0.3.0")
```

_NOTE_ this plugin targets sbt 0.13.6+

You will need to add the following to your `project/build.properties` file if you have multiple versions of sbt installed

    sbt.version=0.13.8

Be sure to use the [latest launcher](http://www.scala-sbt.org/download.html)

## Resolving bintray artifacts

Using sbt 0.13.6+, you don't need this plugin to resolve bintray-hosted dependencies.

```scala
resolvers += Resolver.jcenterRepo
```

This will add JCenter (the [analog to maven central for bintray](https://bintray.com/bintray/jcenter)) to your resolver chain. JCenter is a bintray aggregation repository. You will find most of what you want there. If you wish to add your package to this repository, just link it!

So you want to resolve a package from someone else's repo? Not a problem. Add the following to your sbt build definition

```scala
resolvers += Resolver.bintrayRepo("otherUser", "otherRepo")
```

Typically you will be depending on packages published to that users's "maven" repo

```scala
resolvers += Resolver.bintrayRepo("otherUser", "maven")
```

## Usage

### Publishing

To publish a package to bintray, you need a bintray account. You can do so [here](https://bintray.com/signup/index). 
`BintrayPlugin` is an auto plugin that will be added to all projects in your build.
There are two steps to publish a "version" of a bintray package:

1. First, stage all artifacts using `publish`.
2. Once all artifacts are staged, run `bintrayRelease` to make the artifacts public

If you try to publish at this point, you will be prompted for your bintray username and api key. This will generate an sbt credentials
file under `~/.bintray/.credentials` used to authenticate publishing requests to bintray.

You can interactively change to bintray credentials used by sbt anytime with

    > bintrayChangeCredentials

Note you will need to reload your project afterwards which will reset your `publishTo` setting.

At any time you can check who you will be authenticated as with the `bintrayWhoami` setting which will print your bintray username

    > bintrayWhoami

You may optionally wish to publish to a bintray organization instead of your bintray user account. To do so, use the `bintrayOrganization` settting
in your project's build definition.

```scala
bintrayOrganization := Some("strength-in-numbers")
```

#### Licenses

Bintray requires a license with a name listed [here](https://bintray.com/docs/api/#_footnote_1). If you are new to software licenses you may
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

In addition to labels, you can also assign metadata attributes that expose information to package tooling. These can be assigned at the package and the version levels. By default, this plugin assigns a flag indicating "this is an sbt plugin" to the package and the scala version and optionally sbt version to the package version. You can assign these with the `packageAttributes in bintray` and `versionAttributes in bintray` setting keys. These values must be typed and conform to the [types](https://github.com/softprops/bintry#metadata) bintray [exposes](https://bintray.com/docs/api.html#_attributes).

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
with using your project's (module)name as the package name and description for your package description.

### Unpublishing

It's generally a bad practice to remove a version of a library others may depend on but sometimes you may want test a release with the ability to immediately take it back down if something goes south before others start depending on it. Bintray allows for this flexibility and thus, bintray-sbt does as well. Use the `unpublish` task to unpublish the current version from bintray.

    > bintrayUnpublish

### Finding your way around

The easiest way to learn about bintray-sbt is to use the sbt shell typing `bintray<tab>` or `help bintray` to discover bintray keys.

Doug Tangren (softprops) 2013-2014

always be shipping.
