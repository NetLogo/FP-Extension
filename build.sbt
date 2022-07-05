import org.nlogo.build.NetLogoExtension

enablePlugins(NetLogoExtension)

name       := "fp"
version    := "1.0.0"
isSnapshot := true

netLogoClassManager := "org.nlogo.extensions.fp.FunctionalProgrammingExtension"
netLogoVersion      := "6.2.2-5434ea7"
netLogoZipExtras    ++= Seq(baseDirectory.value / "FP Example.nlogo")

scalaVersion          := "2.12.15"
scalacOptions        ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings", "-Xlint", "-encoding", "us-ascii")
Compile / scalaSource := baseDirectory.value / "src" / "main"
Test    / scalaSource := baseDirectory.value / "src" / "test"
