package jejutic.onlinejudge.gateway.config

import jejutic.onlinejudge.gateway.domain.authorization.SecretKey

final case class AuthConfig(
  encryptionKey: SecretKey,
  expirationSeconds: Long,
  saltCost: Int
)
