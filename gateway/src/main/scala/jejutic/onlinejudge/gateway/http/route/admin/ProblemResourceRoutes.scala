package jejutic.onlinejudge.gateway.http.route.admin

import cats.effect.kernel.Concurrent
import cats.syntax.all.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import jejutic.onlinejudge.gateway.domain.{Problem, User}
import jejutic.onlinejudge.gateway.service.ProblemService
import org.http4s.circe.*
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, EntityDecoder, HttpRoutes}
import org.typelevel.log4cats.Logger

class ProblemResourceRoutes[F[_]: Concurrent: Logger](
  authMiddleware: AuthMiddleware[F, User],
  problemService: ProblemService[F]
) extends AbstractResourceRoutes[F] {

  private val prefixPath = "/problem"

  given EntityDecoder[F, Problem] = jsonOf

  private val httpRoutes: AuthedRoutes[User, F] =
    AuthedRoutes.of { case authedReq @ POST -> Root as _ =>
      checkAdmin(authedReq) {
        for {
          newProblem <- authedReq.req.as[Problem]
          id <- problemService
            .createProblem(newProblem)
          response <- Ok(id.asJson)
        } yield response
      }
    }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
