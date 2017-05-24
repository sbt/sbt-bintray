ScriptedPlugin.scriptedSettings

scriptedLaunchOpts := { scriptedLaunchOpts.value ++
  Seq("-Xmx1024M", "-Dplugin.version=" + version.value, "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005")
}

scriptedBufferLog := false