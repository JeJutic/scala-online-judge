package jejutic.onlinejudge.gateway.http.route.secured

import cats.effect.kernel.Concurrent
import cats.syntax.all.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import jejutic.onlinejudge.gateway.domain.User
import jejutic.onlinejudge.gateway.http.route.AbstractRoutes
import jejutic.onlinejudge.gateway.service.SubmissionService
import org.http4s.circe.*
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, HttpRoutes}
import org.typelevel.log4cats.Logger

class SubmissionRoutes[F[_]: Concurrent: Logger](
  authMiddleware: AuthMiddleware[F, User],
  submissionService: SubmissionService[F]
) extends AbstractRoutes[F] {

  private val prefixPath = "submission"

  private val httpRoutes: AuthedRoutes[User, F] =
    AuthedRoutes.of {
      case GET -> Root / LongVar(submissionId) as user =>
        for {
          option <- submissionService
            .checkSubmission(user, submissionId)
          response <- option.fold(
            NotFound()
          )(submission => Ok(submission.asJson))
        } yield response

      case GET -> Root as user =>
        for {
          ids <- submissionService
            .listSubmissions(user.username)
          response <- Ok(ids.asJson)
        } yield response

    }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
