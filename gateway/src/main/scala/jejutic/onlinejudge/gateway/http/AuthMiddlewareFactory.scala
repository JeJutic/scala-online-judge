package jejutic.onlinejudge.gateway.http

import cats.Monad
import cats.data.{EitherT, Kleisli, OptionT}
import cats.syntax.all.*
import jejutic.onlinejudge.gateway.domain.User
import jejutic.onlinejudge.gateway.service.{JwtCoder, UserService}
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization
import org.http4s.server.AuthMiddleware
import org.http4s.{AuthedRoutes, Request}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.syntax.*

class AuthMiddlewareFactory[F[_]: Monad: Logger](
  jwtCoder: JwtCoder[F],
  userService: UserService[F]
) extends Http4sDsl[F] {

  val authMiddleware: AuthMiddleware[F, User] = {

    // untyped error bc it's immediately utilized in `onFailure`
    val authUserHeader: Kleisli[F, Request[F], Either[String, User]] = Kleisli { request =>
      (for {
        headers <- EitherT.fromEither(
          request.headers
            .get(Authorization.name)
            .toRight("Couldn't find an Authorization header")
        )
        token <- EitherT(
          jwtCoder
            .decode(headers.head.value)
            .map(_.toEither.left.map(_.getMessage))
        )
        username <- EitherT.fromEither(
          token.subject
            .toRight("JWT token format is invalid")
        )
        user <- EitherT(
          userService
            .retrieve(username)
            .map(_.toRight("User not found"))
        )
      } yield user).leftSemiflatMap { errorMsg =>
        for {
          _ <- debug"Auth error for $request: $errorMsg"
        } yield errorMsg
      }.value
    }

    val onFailure: AuthedRoutes[String, F] =
      Kleisli(req => OptionT.liftF(Forbidden(req.context)))

    AuthMiddleware(authUserHeader, onFailure)
  }

}
