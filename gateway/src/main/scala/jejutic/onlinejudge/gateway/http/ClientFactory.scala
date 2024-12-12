package jejutic.onlinejudge.gateway.http

import cats.effect.kernel.{Async, Resource}
import fs2.io.net.Network
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder

object ClientFactory {

  def client[F[_]: Async: Network]: Resource[F, Client[F]] =
    EmberClientBuilder
      .default[F]
      .build

}
