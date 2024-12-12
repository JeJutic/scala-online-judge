package jejutic.onlinejudge.gateway.service

import at.favre.lib.crypto.bcrypt.BCrypt
import cats.effect.kernel.Sync
import jejutic.onlinejudge.gateway.config.AuthConfig
import jejutic.onlinejudge.gateway.domain.authorization.{Password, SaltedPassword}

trait PasswordSalter[F[_]] {

  def salt(password: Password): F[SaltedPassword]

  def verify(password: Password, saltedPassword: SaltedPassword): F[Boolean]

}

object PasswordSalter {

  def make[F[_]: Sync](
    cfg: AuthConfig
  ): PasswordSalter[F] = new PasswordSalter[F] {
    override def salt(password: Password): F[SaltedPassword] =
      Sync[F].blocking(
        BCrypt.withDefaults
          .hashToString(
            cfg.saltCost,
            password.toCharArray
          )
      )

    override def verify(
      password: Password,
      saltedPassword: SaltedPassword
    ): F[Boolean] =
      Sync[F].blocking(
        BCrypt
          .verifyer()
          .verify(
            password.toCharArray,
            saltedPassword
          )
          .verified
      )
  }

}
