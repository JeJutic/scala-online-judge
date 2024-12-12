package jejutic.onlinejudge.gateway.http.route.secured

import cats.effect.kernel.Concurrent
import cats.syntax.all.*
import io.circe.generic.auto.*
import io.circe.syntax.*
import jejutic.onlinejudge.gateway.domain.Submission.NewSubmission
import jejutic.onlinejudge.gateway.domain.User
import jejutic.onlinejudge.gateway.domain.User.Participant
import jejutic.onlinejudge.gateway.domain.error.PersistenceError.NotFoundError.{
  ProblemNotFound,
  SubmissionNotFound
}
import jejutic.onlinejudge.gateway.http.route.AbstractRoutes
import jejutic.onlinejudge.gateway.service.{ProblemService, SubmissionService}
import org.http4s.circe.*
import org.http4s.server.{AuthMiddleware, Router}
import org.http4s.{AuthedRoutes, EntityDecoder, HttpRoutes}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.syntax.LoggerInterpolator

class ProblemRoutes[F[_]: Concurrent: Logger](
  authMiddleware: AuthMiddleware[F, User],
  problemService: ProblemService[F],
  submissionService: SubmissionService[F]
) extends AbstractRoutes[F] {

  private val prefixPath = "problem"

  given decoder: EntityDecoder[F, NewSubmission] = jsonOf

  private val httpRoutes: AuthedRoutes[User, F] =
    AuthedRoutes.of {
      case GET -> Root / LongVar(problemId) as _ =>
        for {
          problemOption <- problemService.findProblem(problemId)
          response <- problemOption.fold(
            NotFound()
          )(problem => Ok(problem.asJson))
        } yield response

      case GET -> Root as _ =>
        for {
          ids      <- problemService.findAll()
          response <- Ok(ids.asJson)
        } yield response

      case authedReq @ POST -> Root / LongVar(problemId) as user =>
        user match
          case Participant(username, _) =>
            for {
              newSubmission <- authedReq.req.as[NewSubmission]
              either <- submissionService
                .submit(username, problemId, newSubmission)
              response <- either
                .fold(
                  _ match
                    case ProblemNotFound(id) =>
                      NotFound(s"Problem $id not found")
                    case SubmissionNotFound(id) =>
                      for {
                        _    <- error"Submission id inconsistency: $id"
                        resp <- InternalServerError()
                      } yield resp,
                  id => Ok(id.asJson)
                )
            } yield response
          case _ => forbidden(authedReq)

    }

  val routes: HttpRoutes[F] = Router(
    prefixPath -> authMiddleware(httpRoutes)
  )

}
