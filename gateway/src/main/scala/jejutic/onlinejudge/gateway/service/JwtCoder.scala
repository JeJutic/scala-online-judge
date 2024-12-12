package jejutic.onlinejudge.gateway.service

import cats.effect.kernel.Sync
import cats.implicits.*
import jejutic.onlinejudge.gateway.config.AuthConfig
import jejutic.onlinejudge.gateway.domain.authorization.{Token, UserName}
import pdi.jwt.{Jwt, JwtAlgorithm, JwtClaim}

import scala.util.Try

trait JwtCoder[F[_]] {

  def encode(username: UserName): F[Token]

  def decode(token: String): F[Try[JwtClaim]]

}

object JwtCoder {

  def make[F[_]: Sync](
    now: Now[F],
    cfg: AuthConfig
  ): JwtCoder[F] = new JwtCoder[F] {
    override def encode(username: UserName): F[Token] =
      for {
        clock    <- now.getClock
        issuedAt <- Sync[F].delay(clock.millis()) // an effect
        issuedAtSeconds = issuedAt / 1000
        claim = JwtClaim()
          .about(username) // subject field
          .issuedAt(issuedAtSeconds)
          .expiresAt(issuedAtSeconds + cfg.expirationSeconds)
      } yield Jwt.encode(claim, cfg.encryptionKey, JwtAlgorithm.HS512)

    override def decode(token: String): F[Try[JwtClaim]] =
      now.getClock.flatMap { clock =>
        Sync[F].delay( // uses clock inside => has a side effect
          Jwt(clock)
            .decode(token, cfg.encryptionKey, Seq(JwtAlgorithm.HS512))
        )
      }
  }

}
