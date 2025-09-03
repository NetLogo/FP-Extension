import org.nlogo.build.NetLogoExtension

enablePlugins(NetLogoExtension)

name       := "fp"
version    := "1.1.1"
isSnapshot := true

scalaVersion           := "3.7.0"
Compile / scalaSource  := baseDirectory.value / "src" / "main"
Test / scalaSource     := baseDirectory.value / "src" / "test"
scalacOptions          ++= Seq("-deprecation", "-unchecked", "-Xfatal-warnings", "-encoding", "us-ascii")

netLogoExtName      := "fp"
netLogoClassManager := "org.nlogo.extensions.fp.FunctionalProgrammingExtension"
netLogoVersion      := "7.0.0-beta1"
netLogoZipExtras    ++= Seq(baseDirectory.value / "FP Example.nlogo")
