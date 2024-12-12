package jejutic.onlinejudge.gateway.repository.algebra

import jejutic.onlinejudge.gateway.domain.authorization.{SaltedPassword, UserName}

trait UserRepository[F[_]] {

  def find(username: UserName): F[Option[SaltedPassword]]

}
