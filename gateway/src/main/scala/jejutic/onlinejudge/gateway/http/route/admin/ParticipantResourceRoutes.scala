package jejutic.onlinejudge.gateway.http.route.admin

import cats.effect.kernel.Concurrent
import cats.syntax.all.*
import io.circe.generic.auto.*
import io.circe.refined.*
import jejutic.onlinejudge.gateway.domain.User
import jejutic.onlinejudge.gateway.domain.User.NewParticipant
import jejutic.onlinejudge.gateway.domain.error.PersistenceError.AlreadyExistsError.{
  EmailAlreadyExists,
  UsernameAlreadyExists
}
import jejutic.onlinejudge.gateway.service.ParticipantService
import org.http4s.circe.jsonOf
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, EntityDecoder, HttpRoutes}
import org.typelevel.log4cats.Logger

class ParticipantResourceRoutes[F[_]: Concurrent: Logger](
  authMiddleware: AuthMiddleware[F, User],
  participantService: ParticipantService[F]
) extends AbstractResourceRoutes[F] {

  private val prefixPath = "/participant"

  given EntityDecoder[F, NewParticipant] = jsonOf

  private val httpRoutes: AuthedRoutes[User, F] =
    AuthedRoutes.of { case authedReq @ POST -> Root as _ =>
      checkAdmin(authedReq) {
        for {
          newParticipant <- authedReq.req.as[NewParticipant]
          either <- participantService
            .addParticipant(newParticipant)
          response <- either
            .fold(
              _ match
                case _: UsernameAlreadyExists => Conflict("username already exists")
                case _: EmailAlreadyExists    => Conflict("email already exists"),
              _ => Ok()
            )
        } yield response
      }
    }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
