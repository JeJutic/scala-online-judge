package jejutic.onlinejudge.gateway.service

import cats.Applicative

import java.time.Clock

trait Now[F[_]] {
  // yes we really need Clock from java.time, not from cats-effect
  def getClock: F[Clock]
}

object Now {

  def make[F[_]: Applicative](): Now[F] =
    new Now[F] {

      // has no side effect so ig ok to use `pure`
      override def getClock: F[Clock] =
        Applicative[F].pure(Clock.systemUTC())

    }

}
