package jejutic.onlinejudge.gateway.http.route.admin

import cats.Monad
import jejutic.onlinejudge.gateway.domain.User
import jejutic.onlinejudge.gateway.domain.User.Admin
import jejutic.onlinejudge.gateway.http.route.AbstractRoutes
import org.http4s.{AuthedRequest, Response}
import org.typelevel.log4cats.Logger

class AbstractResourceRoutes[F[_]: Monad: Logger] extends AbstractRoutes[F] {

  protected def checkAdmin[T](authedRequest: AuthedRequest[F, User])(
    f: => F[Response[F]]
  ): F[Response[F]] =
    authedRequest.context match {
      case Admin(_) => f
      case _        => forbidden(authedRequest)
    }

}
