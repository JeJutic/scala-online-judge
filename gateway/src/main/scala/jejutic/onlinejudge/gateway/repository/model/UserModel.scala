package jejutic.onlinejudge.gateway.repository.model

import jejutic.onlinejudge.gateway.domain.authorization.{SaltedPassword, UserName}

final case class UserModel(
  name: UserName,
  saltedPassword: SaltedPassword
)
