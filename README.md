# bintray sbt

an sbt interface for publishing and resolving bintray packages

## install

todo

## usage

### Resolving

If you only need to resolve bintray hosted dependencies you can just add

```scala
seq(bintrayResolverSettings:_*)
```

to your build. This will add `bintray.Opts.resolver.jcenter` (the analog to maven central for bintray) to your resolver chain.

### Publishing

To publish to bintray you need a bintray account. After creating a bintray account you can add

```scala
seq(bintrayPublishSettings:_*)
```

To your build. Afterwards you will be prompted for your bintray username and api key. This will generate an sbt credentials
file under `~/.bintray/.credentials` used to authenticate publishing requests. 


### Both

You can save yourself some configuration if you wish to both resolve and publish by simply adding the following to your build configuration

```scala
seq(bintraySettings:_*)
```

Doug Tangren (softprops) 2013
