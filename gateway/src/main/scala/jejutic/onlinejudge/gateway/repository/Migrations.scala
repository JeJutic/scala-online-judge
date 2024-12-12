package jejutic.onlinejudge.gateway.repository

import cats.data.EitherT
import cats.effect.Sync
import jejutic.onlinejudge.gateway.config.DbConfig
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.Location
import org.flywaydb.core.api.configuration.FluentConfiguration
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.syntax.*

import scala.jdk.CollectionConverters.*

// code taken from https://alexn.org/blog/2020/11/15/managing-database-migrations-scala/
object Migrations {

  def migrate[F[_]: Sync: Logger](config: DbConfig): F[Either[String, Unit]] =
    (for {
      _ <- EitherT.liftF(
        info"Running migrations from locations: ${config.migrationsLocations.mkString(", ")}"
      )
      count <- EitherT(Sync[F].delay(unsafeMigrate(config)))
      _     <- EitherT.liftF(info"Executed $count migrations")
    } yield ()).value

  private def unsafeMigrate(config: DbConfig): Either[String, Int] = {
    val m: FluentConfiguration = Flyway.configure
      .dataSource(
        config.url,
        config.user,
        config.password
      )
      .group(true)
      .outOfOrder(false)
      .table(config.migrationsTable)
      .locations(
        config.migrationsLocations
          .map(new Location(_))*
      )
      .baselineOnMigrate(true)

    logValidationErrorsIfAny(m).map(_ => m.load().migrate().migrationsExecuted)
  }

  private def logValidationErrorsIfAny(m: FluentConfiguration): Either[String, Unit] = {
    val validated = m
      .ignoreMigrationPatterns("*:pending")
      .load()
      .validateWithResult()

    if (!validated.validationSuccessful)
      Left(validated.invalidMigrations.asScala.map { error =>
        s"""
           |Failed validation:
           |  - version: ${error.version}
           |  - path: ${error.filepath}
           |  - description: ${error.description}
           |  - errorCode: ${error.errorDetails.errorCode}
           |  - errorMessage: ${error.errorDetails.errorMessage}
                """.stripMargin.strip
      }.mkString)
    else Right(())
  }

}
