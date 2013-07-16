# bintray sbt

an sbt interface for publishing and resolving [bintray](https://bintray.com) packages.

always be shipin`

## install

## what you need

- an account on [bintray](https://bintray.com) (get one [here](https://bintray.com/signup/index))
- a desire to build the scala community

Add the following to your sbt `project/plugins.sbt` file

```scala
addSbtPlugin("me.lessis" % "bintray-sbt" % "0.1.0")
```

## usage

### Resolving

If you only need to resolve bintray hosted dependencies you can just add

```scala
seq(bintrayResolverSettings:_*)
```

to your build. This will add `bintray.Opts.resolver.jcenter` (the [analog to maven central for bintray](https://bintray.com/bintray/jcenter)) to your resolver chain. JCenter is a bintray aggregation repository. You will find most of what you want there. If you wish to add your package to this repository, just link it!

So you want to resolve a package from someone else's repo? Not a problem. Add the following to your sbt build definition

```scala
resolvers += bintray.Opts.resolver.repo(user, repo)
```

### Publishing

To publish a package to bintray, you need a bintray account. After creating a bintray account you can add

```scala
seq(bintrayPublishSettings:_*)
```

To your build. If you try to publish at this point, you will be prompted for your bintray username and api key. This will generate an sbt credentials
file under `~/.bintray/.credentials` used to authenticate publishing requests to bintray.

You can interactively change to bintray credentials used by sbt anytime with

    > bintray:changeCredentials

Note you will need to reload your project afterwards which will reset your `publishTo` setting.

#### Licenses

Bintray requires a license with a name listed [here](https://bintray.com/docs/api.html#_footnote_1). If you are new to software licenses you may 
want to grab a coffee and absorb some [well organized information](http://choosealicense.com/) on the topic of choice.
Sbt already defines a `licenses` setting key. In order to use bintray sbt you must define your `licenses` key to contain a license with a name matching
one of those bintray defines. I recommend [MIT](http://choosealicense.com/licenses/mit/).


```scala
licenses += ("MIT", url("http://opensource.org/licenses/MIT"))
```

#### Labels

The first time you publish a bintray package, this plugin will create the package for you on bintrary. Along with the actual contents
of the package, you can list a publicly visible list of labels that related to your package.

You can assign this with the `packageLabels in bintray` setting key.

```scala
bintray.Keys.packageLabels in bintray.Keys.bintray := Seq("hipster", "keen")
```

#### Metadata

In addition to labels, you can also assign metadata attributes that expose information to package tooling tooling. These can be assigned at the package and the version levels. By default, this plugin assigns the a flag indicating this is an sbt plugin to the package and the scala version and optionally sbt version to the package version. You can assign these with the with `packageAttributes in bintray` and `versionAttributes in bintray` setting keys. These values must be typed an conform to the the [types](https://github.com/softprops/bintry#metadata) bintray [exposes](https://bintray.com/docs/api.html#_attributes).

```scala
// append custom package attributes
bintray.Keys.packageAttributes in bintray.Keys.bintray ~=
  ((_: bintray.AttrMap) ++ Map("my-package-attr" -> Seq(bintry.StringAttr("my-value"))))
```

```scala
// append custom version attributes
bintray.Keys.versionAttributes in bintray.Keys.bintray ~=
  ((_: bintray.AttrMap) ++ Map("my-version-attr" -> Seq(bintry.BooleanAttr(true))))
```

Note, This interface will likely be subject to change in the future. All changes will be announced and well documented.

##### Other pieces of flair

When publishing for the first time, bintray sbt will create a package for you under your bintray accounts maven repository.
With using your projects name as the package name and description for your package description. 

### Both Resolving and Publishing

You can save yourself some configuration if you wish to both resolve and publish by simply adding the following to your build configuration

```scala
seq(bintraySettings:_*)
```

### finding your way around

The easiest way to learn about bintry-sbt is to use the sbt console REPL typing `bintray::<tab>` to discover bintray keys.

Doug Tangren (softprops) 2013
