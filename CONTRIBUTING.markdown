### releasing steps

```
jenv shell 1.7
sbt
> publishSigned
jenv shell 1.8
# uncomment build.sbt parts
sbt
> publishSigned
```
