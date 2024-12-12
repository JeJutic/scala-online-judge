import sbt.*

object Dependencies {

  val catsCore   = "org.typelevel" %% "cats-core"   % "2.12.0"
  val catsEffect = "org.typelevel" %% "cats-effect" % "3.5.4"

  val refined = "eu.timepit" %% "refined" % "0.11.2"

  val zioHttp = "dev.zio" %% "zio-http" % "3.0.1"

  val jwtCore = "com.github.jwt-scala" %% "jwt-core" % "10.0.1"

  val http4sVersion = "0.23.29"

  val http4s: Seq[ModuleID] = Seq(
    "org.http4s" %% "http4s-ember-client" % http4sVersion,
    "org.http4s" %% "http4s-ember-server" % http4sVersion,
    "org.http4s" %% "http4s-dsl"          % http4sVersion
  )

  val fs2RabbitVersion = "5.3.0"

  val fs2Rabbit = "dev.profunktor" %% "fs2-rabbit" % fs2RabbitVersion

  val circe: Seq[ModuleID] = Seq(
    "io.circe"       %% "circe-generic"    % "0.14.10",
    "io.circe"       %% "circe-refined"    % "0.15.1",
    "org.http4s"     %% "http4s-circe"     % http4sVersion,
    "dev.profunktor" %% "fs2-rabbit-circe" % fs2RabbitVersion
  )

  val pureConfig = "com.github.pureconfig" %% "pureconfig-generic-scala3" % "0.17.8"

  val slf4j: Seq[ModuleID] = Seq(
    "org.typelevel" %% "log4cats-slf4j"  % "2.7.0",
    "ch.qos.logback" % "logback-classic" % "1.5.12"
  )

  val doobieVersion = "1.0.0-RC6"

  val doobie: Seq[ModuleID] = Seq(
    "org.tpolecat" %% "doobie-hikari"   % doobieVersion,
    "org.tpolecat" %% "doobie-postgres" % doobieVersion
//    "org.tpolecat" %% "doobie-specs2"    % "1.0.0-RC4" % "test",
//    "org.tpolecat" %% "doobie-scalatest" % "1.0.0-RC4" % "test"
  )

  val flywayPostgres = "org.flywaydb" % "flyway-database-postgresql" % "11.0.0"

  val bcrypt = "at.favre.lib" % "bcrypt" % "0.10.2"

}
