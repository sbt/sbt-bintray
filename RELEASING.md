### releasing steps

```
jenv shell 1.7
sbt
> publish

jenv shell 1.8
sbt
> ++2.12.2
> ^^1.0.0-RC2
> publish
```
