package jejutic.onlinejudge.gateway.http.route

import cats.Monad
import cats.syntax.all.*
import jejutic.onlinejudge.gateway.domain.User
import org.http4s.dsl.Http4sDsl
import org.http4s.{AuthedRequest, Response}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.syntax.*

class AbstractRoutes[F[_]: Monad: Logger] extends Http4sDsl[F] {

  protected def forbidden[T](authedRequest: AuthedRequest[F, User]): F[Response[F]] =
    for {
      _        <- debug"Access forbidden for $authedRequest"
      response <- Forbidden()
    } yield response

}
