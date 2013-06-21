# bintray sbt

an sbt interface for publishing and resolving [bintray](https://bintray.com) packages

## install

## what you need

- an account on [bintray](https://bintray.com) (get one [here](https://bintray.com/signup/index))
- a desire to build the scala community

todo

## usage

### Resolving

If you only need to resolve bintray hosted dependencies you can just add

```scala
seq(bintrayResolverSettings:_*)
```

to your build. This will add `bintray.Opts.resolver.jcenter` (the [analog to maven central for bintray](https://bintray.com/bintray/jcenter)) to your resolver chain. JCenter is a bintray aggregation repository. You will find most of what you want there.

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

### Both

You can save yourself some configuration if you wish to both resolve and publish by simply adding the following to your build configuration

```scala
seq(bintraySettings:_*)
```

Doug Tangren (softprops) 2013
