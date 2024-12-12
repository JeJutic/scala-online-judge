package jejutic.onlinejudge.core

import cats.data.EitherT
import cats.effect.kernel.{Async, Sync}
import cats.effect.std.Console
import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.all.*
import fs2.io.net.Network
import jejutic.onlinejudge.core.config.ApplicationConfig
import jejutic.onlinejudge.core.queue.ClientFactory
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object App extends IOApp {

  private given [F[_]: Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  private def applicationFromConfig[F[_]: Async: Logger](
    config: ApplicationConfig
  ): F[Unit] =
    ClientFactory(config.rabbitClient).producerConsumer
      .use { case (producer, (acker, consumer)) =>
        StubWorker(
          config.processingTime,
          producer,
          acker,
          consumer
        ).run
      }

  private def application[F[_]: Async: Network: Console: Logger]: F[ExitCode] =
    (for {
      config <- EitherT(ApplicationConfig.load())
      _      <- EitherT.liftF(applicationFromConfig(config))
    } yield ()).value.attempt
      .map(_.left.map(_.getMessage))
      .map(_.joinRight)
      .flatMap {
        case Left(errorMsg) =>
          Console[F]
            .println(s"Exit with error: $errorMsg")
            .as(ExitCode.Error)
        case Right(_) => Async[F].pure(ExitCode.Success)
      }

  override def run(args: List[String]): IO[ExitCode] =
    application[IO]

}
