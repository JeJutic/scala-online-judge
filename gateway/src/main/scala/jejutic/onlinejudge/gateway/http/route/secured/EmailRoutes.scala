package jejutic.onlinejudge.gateway.http.route.secured

import cats.Monad
import cats.syntax.all.*
import jejutic.onlinejudge.gateway.domain.User
import jejutic.onlinejudge.gateway.service.ParticipantService
import org.http4s.dsl.Http4sDsl
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}

class EmailRoutes[F[_]: Monad](
  authMiddleware: AuthMiddleware[F, User],
  participantService: ParticipantService[F]
) extends Http4sDsl[F] {

  private val prefixPath = "email"

  private val httpRoutes: AuthedRoutes[User, F] =
    AuthedRoutes.of { case GET -> Root / username as _ =>
      for {
        emailOption <- participantService.getEmail(username)
        response <- emailOption.fold(
          NotFound()
        )(Ok(_))
      } yield response
    }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
