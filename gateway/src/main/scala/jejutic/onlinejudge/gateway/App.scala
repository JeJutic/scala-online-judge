package jejutic.onlinejudge.gateway

import cats.data.EitherT
import cats.effect.kernel.{Async, Resource, Sync}
import cats.effect.std.Console
import cats.effect.{ExitCode, IO, IOApp}
import cats.syntax.all.*
import dev.profunktor.fs2rabbit.model.AmqpMessage
import doobie.Transactor
import fs2.io.net.Network
import jejutic.onlinejudge.gateway.config.ApplicationConfig
import jejutic.onlinejudge.gateway.http.route.admin.AdminRoutes
import jejutic.onlinejudge.gateway.http.route.secured.{EmailRoutes, ProblemRoutes, SubmissionRoutes}
import jejutic.onlinejudge.gateway.http.route.{DocsRoutes, LoginRoutes}
import jejutic.onlinejudge.gateway.http.{AuthMiddlewareFactory, ServerFactory}
import jejutic.onlinejudge.gateway.repository.interpreter.*
import jejutic.onlinejudge.gateway.repository.{Migrations, TransactorFactory}
import jejutic.onlinejudge.gateway.service.*
import org.http4s.client.Client
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

object App extends IOApp {

  private given [F[_]: Sync]: Logger[F] = Slf4jLogger.getLogger[F]

  private def serverFromDeps[F[_]: Async: Network: Logger](
    cfg: ApplicationConfig,
    xa: Transactor[F],
    httpClient: Client[F],
    queueProducer: AmqpMessage[String] => F[Unit]
  ): Resource[F, SubmissionResultProcessor[F]] = {
    // repository layer
    val userRepository        = UserRepositoryInterpreter(xa)
    val adminRepository       = AdminRepositoryInterpreter(xa)
    val participantRepository = ParticipantRepositoryInterpreter(xa)
    val problemRepository     = ProblemRepositoryInterpreter(xa)
    val submissionRepository  = SubmissionRepositoryInterpreter(xa)

    // service layer
    val swaggerClient  = SwaggerClient.make(cfg.swaggerClient, httpClient)
    val now            = Now.make()
    val jwtCoder       = JwtCoder.make(now, cfg.auth)
    val passwordSalter = PasswordSalter.make(cfg.auth)
    val userService = UserService.make(
      jwtCoder,
      passwordSalter,
      userRepository,
      adminRepository,
      participantRepository
    )
    val participantService = ParticipantService.make(
      passwordSalter,
      participantRepository
    )
    val problemService      = ProblemService.make(problemRepository)
    val submissionPublisher = SubmissionPublisher.make(queueProducer)
    val submissionService = SubmissionService.make(
      submissionRepository,
      submissionPublisher
    )
    val submissionResultProcessor = SubmissionResultProcessor.make(submissionService)

    // controller layer
    val authMiddleware = AuthMiddlewareFactory(
      jwtCoder,
      userService
    ).authMiddleware
    val loginRoutes = LoginRoutes(userService).routes
    val docsRoutes  = DocsRoutes(swaggerClient).routes
    val adminRoutes = AdminRoutes.routes(
      authMiddleware,
      participantService,
      problemService
    )
    val emailRoutes = EmailRoutes(authMiddleware, participantService).routes
    val problemRoutes = ProblemRoutes(
      authMiddleware,
      problemService,
      submissionService
    ).routes
    val submissionRoutes = SubmissionRoutes(
      authMiddleware,
      submissionService
    ).routes

    val routes = loginRoutes <+>
      docsRoutes <+>
      adminRoutes <+>
      emailRoutes <+>
      problemRoutes <+>
      submissionRoutes
    ServerFactory
      .serverFromRoutes(cfg.server, routes)
      .as(submissionResultProcessor)
  }

  private def applicationFromConfig[F[_]: Async: Network: Logger](
    config: ApplicationConfig
  ): F[Unit] =
    (for {
      xa               <- TransactorFactory.transactor(config.database)
      httpClient       <- http.ClientFactory.client
      producerConsumer <- queue.ClientFactory(config.rabbitClient).producerConsumer
      (producer, ackerConsumer) = producerConsumer
      resultProcessor <- serverFromDeps(config, xa, httpClient, producer)
    } yield (resultProcessor, ackerConsumer))
      .use { case (resultProcessor, ackerConsumer) =>
        resultProcessor.process(ackerConsumer)
      }

  private def application[F[_]: Async: Network: Console: Logger]: F[ExitCode] =
    (for {
      config <- EitherT(ApplicationConfig.load())
      _      <- EitherT(Migrations.migrate(config.database))
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
