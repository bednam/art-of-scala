import Dependencies._

ThisBuild / scalaVersion     := "2.13.10"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

addCompilerPlugin(
    "org.typelevel" % "kind-projector" % "0.13.2" cross CrossVersion.full
)

lazy val root = (project in file("."))
  .settings(
    name := "the-art-of-scala",
    libraryDependencies += "org.typelevel" %% "cats-effect" % "3.5.0",
    libraryDependencies += "org.typelevel" %% "cats-mtl" % "1.3.1",
    libraryDependencies += "org.typelevel" %% "log4cats-core" % "2.6.0",
    libraryDependencies += "org.typelevel" %% "log4cats-slf4j" % "2.6.0",
    libraryDependencies += "org.typelevel" %% "cats-core" % "2.9.0",
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.4.6"
  )

// See https://www.scala-sbt.org/1.x/docs/Using-Sonatype.html for instructions on how to publish to Sonatype.
