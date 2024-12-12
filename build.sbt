import Dependencies.*
import sbt.Keys.fork

ThisBuild / scalaVersion := "3.5.2"
ThisBuild / version      := "0.1.0-SNAPSHOT"

lazy val root: Project = project
  .in(file("."))
  .settings(
    name := "online-judge"
  )
  .aggregate(
    core,
    gateway
  )

lazy val core = project
  .settings(commonSettings)
  .enablePlugins(
    JavaAppPackaging,
    DockerPlugin
  )
  .settings(
    Docker / packageName := "jejutic/onlinejudge-core-stub",
    libraryDependencies ++= Seq(
      catsCore,
      catsEffect,
      fs2Rabbit,
      pureConfig
    ) ++
      slf4j ++
      circe
  )

lazy val gateway = project
  .settings(commonSettings)
  .enablePlugins(
    JavaAppPackaging,
    DockerPlugin
  )
  .settings(
    Docker / packageName := "jejutic/onlinejudge-gateway",
    dockerExposedPorts ++= Seq(8080),
    libraryDependencies ++= Seq(
      catsCore,
      catsEffect,
      refined,
      jwtCore,
      fs2Rabbit,
      pureConfig,
      flywayPostgres,
      bcrypt
    ) ++
      http4s ++
      circe ++
      slf4j ++
      doobie
  )

lazy val commonSettings = Seq(
  dockerBaseImage    := "openjdk:17",
  dockerUpdateLatest := true,
  scalacOptions ++= Seq(
    "-Werror",
    "-Wunused:all",
    "-Wvalue-discard",
    "-unchecked",
    "-deprecation",
    "-Xmax-inlines", // for application config
    "64"
  ),
  Compile / run / fork := true,
  Test / fork          := true
)
