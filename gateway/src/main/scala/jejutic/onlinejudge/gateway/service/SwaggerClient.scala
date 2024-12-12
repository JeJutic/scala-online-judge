package jejutic.onlinejudge.gateway.service

import cats.effect.kernel.Concurrent
import jejutic.onlinejudge.gateway.config.SwaggerClientConfig
import org.http4s.Uri.{Authority, Path, RegName}
import org.http4s.client.Client
import org.http4s.{Request, Response}

trait SwaggerClient[F[_]] {
  def proxy(request: Request[F], restPath: Path): F[Response[F]]
}

object SwaggerClient {

  def make[F[_]: Concurrent](
    cfg: SwaggerClientConfig,
    client: Client[F]
  ): SwaggerClient[F] =
    new SwaggerClient[F] {

      override def proxy(request: Request[F], restPath: Path): F[Response[F]] = {
        val newAuthority = Authority(
          host = RegName(cfg.host),
          port = Some(cfg.port)
        )
        val proxiedReq =
          request.withUri(
            request.uri.copy(
              authority = Some(newAuthority),
              path = restPath
            )
          )
        client
          .run(proxiedReq)
          .use(
            _.toStrict(None) // otherwise connection can be released without fetching body
          )
      }

    }

}
