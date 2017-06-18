ScriptedPlugin.scriptedSettings
scriptedLaunchOpts := { scriptedLaunchOpts.value ++ Seq(
  "-Xmx1024M",
  "-XX:MaxPermSize=256M",
  "-Dbintray.user=username",
  "-Dbintray.pass=password",
  "-Dplugin.version=" + version.value)
}
scriptedBufferLog := false
