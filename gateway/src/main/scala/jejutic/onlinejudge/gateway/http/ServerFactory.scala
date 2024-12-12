package jejutic.onlinejudge.gateway.http

import cats.data.OptionT
import cats.effect.kernel.{Async, Resource}
import fs2.io.net.Network
import jejutic.onlinejudge.gateway.config.ServerConfig
import org.http4s.HttpRoutes
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Server
import org.http4s.server.middleware.{ErrorAction, ErrorHandling}
import org.typelevel.log4cats.Logger

object ServerFactory {

  def serverFromRoutes[F[_]: Async: Network: Logger](
    cfg: ServerConfig,
    routes: HttpRoutes[F]
  ): Resource[F, Server] = {

    def clientErrorHandler(t: Throwable, msg: => String): OptionT[F, Unit] =
      OptionT.liftF(Logger[F].debug(t)(msg))

    def serviceErrorHandler(t: Throwable, msg: => String): OptionT[F, Unit] =
      OptionT.liftF(Logger[F].error(t)(msg))

    val withErrorLogging = ErrorHandling.Recover.total(
      ErrorAction.log(
        routes,
        messageFailureLogAction = clientErrorHandler,
        serviceErrorLogAction = serviceErrorHandler
      )
    )

    val app = withErrorLogging.orNotFound
    EmberServerBuilder
      .default[F]
      .withHost(cfg.host)
      .withPort(cfg.port)
      .withHttpApp(app)
      .build
  }

}
