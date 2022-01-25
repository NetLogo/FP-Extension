enablePlugins(org.nlogo.build.NetLogoExtension)

name := "fp"

version := "0.0.1"

isSnapshot := true

netLogoExtName := "fp"

netLogoClassManager := "FunctionalProgrammingExtension"

netLogoZipSources := false

netLogoVersion := "6.2.0-d27b502"

scalaVersion := "2.12.0"

scalaSource in Compile := baseDirectory.value / "src"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings", "-encoding", "us-ascii")

netLogoTarget := org.nlogo.build.NetLogoExtension.directoryTarget(baseDirectory.value)
