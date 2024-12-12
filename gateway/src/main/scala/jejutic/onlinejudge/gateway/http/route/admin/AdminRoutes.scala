package jejutic.onlinejudge.gateway.http.route.admin

import cats.effect.Concurrent
import cats.syntax.all.*
import jejutic.onlinejudge.gateway.domain.User
import jejutic.onlinejudge.gateway.service.{ParticipantService, ProblemService}
import org.http4s.HttpRoutes
import org.http4s.server.{AuthMiddleware, Router}
import org.typelevel.log4cats.Logger

object AdminRoutes {

  private val prefixPath = "/admin"

  def routes[F[_]: Concurrent: Logger](
    authMiddleware: AuthMiddleware[F, User],
    participantService: ParticipantService[F],
    problemService: ProblemService[F]
  ): HttpRoutes[F] = Router(
    prefixPath -> (
      ParticipantResourceRoutes(
        authMiddleware,
        participantService
      ).routes <+>
        ProblemResourceRoutes(
          authMiddleware,
          problemService
        ).routes
    )
  )

}
