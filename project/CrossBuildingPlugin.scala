import sbt.{Def, _}
import Keys._

sealed trait SbtVersionSeries
case object Sbt013 extends SbtVersionSeries
case object Sbt1 extends SbtVersionSeries

object CrossBuildingPlugin extends AutoPlugin {
  override def requires = plugins.JvmPlugin
  override def trigger = allRequirements

  object autoImport {
    val sbtPartV = settingKey[Option[(Int, Int)]]("")
    val sbtVersionSeries = settingKey[SbtVersionSeries]("")
    val crossScripted = inputKey[Unit]("")
  }

  import autoImport._
  override def globalSettings = Seq(
    sbtPartV := CrossVersion partialVersion (sbtVersion in pluginCrossBuild).value,
    sbtVersionSeries := (sbtPartV.value match {
      case Some((0, 13)) => Sbt013
      case Some((1, _)) => Sbt1
      case _ =>
        sys error s"Unhandled sbt version ${(sbtVersion in pluginCrossBuild).value}"
    })
  )

  import sbt.{Def, File, ScriptedPlugin, complete}
  import sbt.ScriptedPlugin._

  override def projectSettings = Seq(
    // See https://github.com/sbt/sbt/issues/3245
    crossScripted := Def.inputTask {
      val _: Unit = scriptedDependencies.value
      val scriptedTests = ScriptedPlugin.scriptedTests.value
      val testDir = sbtTestDirectory.value
      val enabledBuffer = scriptedBufferLog.value
      val launcher = sbtLauncher.value
      val launchOpts = scriptedLaunchOpts.value.toArray
      val args = {
        // It has to be chained to prevent illegal dynamic reference...
        ScriptedPlugin
          .asInstanceOf[{
            def scriptedParser(f: File): complete.Parser[Seq[String]]
          }]
          .scriptedParser(sbtTestDirectory.value)
          .parsed
          .toArray
      }
      try {
        if (sbtVersionSeries.value == Sbt1) {
          scriptedTests
            .asInstanceOf[{
              def run(
                  x1: File,
                  x2: Boolean,
                  x3: Array[String],
                  x4: File,
                  x5: Array[String],
                  x6: java.util.List[File]
              ): Unit
            }]
            .run(testDir,
                 enabledBuffer,
                 args,
                 launcher,
                 launchOpts,
                 new java.util.ArrayList())
        } else {
          scriptedTests
            .asInstanceOf[{
              def run(
                  x1: File,
                  x2: Boolean,
                  x3: Array[String],
                  x4: File,
                  x5: Array[String]
              ): Unit
            }]
            .run(testDir, enabledBuffer, args, launcher, launchOpts)
        }
      } catch {
        case e: java.lang.reflect.InvocationTargetException => throw e.getCause
      }
    }.evaluated
  )
}
