package jejutic.onlinejudge.gateway.http.route

import cats.effect.Concurrent
import cats.syntax.all.*
import io.circe.generic.auto.*
import jejutic.onlinejudge.gateway.domain.authorization.LoginForm
import jejutic.onlinejudge.gateway.domain.error.AuthError
import jejutic.onlinejudge.gateway.service.UserService
import org.http4s.circe.jsonOf
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpRoutes}

class LoginRoutes[F[_]: Concurrent](userService: UserService[F]) extends Http4sDsl[F] {

  given EntityDecoder[F, LoginForm] = jsonOf

  val routes: HttpRoutes[F] = HttpRoutes.of[F] { case req @ POST -> Root / "login" =>
    for {
      loginForm <- req.as[LoginForm]
      response <- userService
        .logIn(loginForm)
        .flatMap(
          _.fold(
            _ match
              case AuthError.IncorrectUsernameOrPassword =>
                Forbidden("Incorrect username or password"),
            Ok(_)
          )
        )
    } yield response

  }

}
