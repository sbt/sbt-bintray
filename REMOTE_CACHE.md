sbt-bintray-remote-cache
========================

requirements
------------

- an account on [bintray](https://bintray.com) (get one [here](https://bintray.com/signup/oss))
- a desire to build a zero-second build

setup
-----

Add the following to your sbt `project/plugins.sbt` file:

![Bintray version](https://img.shields.io/bintray/v/sbt/sbt-plugin-releases/sbt-bintray.svg)

```scala
addSbtPlugin("org.foundweekends" % "sbt-bintray-remote-cache" % "x.y.z")
```

### Bintray repo and package

Go to `https://bintray.com/<your_bintray_user>/` and create a new **Generic** repository with the name **`remote-cache`**.

Next, create a _package_ within the remote-cache repo. The granularity should typically be one package for one build.

### build.sbt

Then in your `build.sbt`:

```scala
ThisBuild / bintrayRemoteCacheOrganization := "your_bintray_user or organization"
ThisBuild / bintrayRemoteCachePackage := "your_package_name"
```

usage
-----

### credentials

To push remote cache, you need to provide Bintray credentials (user name and API key) using a credential file or environment variables.
    
1. Environment variables

sbt-bintray-remote-cache will look for bintray user and pass in the environment variables `BINTRAY_REMOTE_CACHE_USER` and  `BINTRAY_REMOTE_CACHE_PASS`. Note that these are different from sbt-bintray.

2. Credentials file

sbt-bintray-remote-cache will look for a credentials file under `~/.bintray/.credentials` used to authenticate publishing requests to bintray.

### pushing remote cache

From the CI machine, run

```
> pushRemoteCache
```

### cleaning up the old cache

From the CI machine, run

```
> bintrayRemoteCacheCleanOld
```

This will **remove** versions older than a month, while keeping minimum 100 cache entry.
