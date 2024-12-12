package jejutic.onlinejudge.gateway.http.route

import cats.effect.Concurrent
import jejutic.onlinejudge.gateway.service.SwaggerClient
import org.http4s.HttpRoutes
import org.http4s.dsl.Http4sDsl

class DocsRoutes[F[_]: Concurrent](client: SwaggerClient[F]) extends Http4sDsl[F] {

  val routes: HttpRoutes[F] = HttpRoutes.of[F] { case req @ GET -> "docs" /: rest =>
    client.proxy(req, rest)
  }

}
