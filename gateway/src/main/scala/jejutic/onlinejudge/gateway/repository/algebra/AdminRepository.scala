package jejutic.onlinejudge.gateway.repository.algebra

import jejutic.onlinejudge.gateway.domain.authorization.UserName

trait AdminRepository[F[_]] {

  // no need for a model for admin
  def find(username: UserName): F[Boolean]

}
