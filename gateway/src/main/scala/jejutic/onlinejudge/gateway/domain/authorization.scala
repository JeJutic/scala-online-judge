package jejutic.onlinejudge.gateway.domain

object authorization {

  type UserName       = String
  type Password       = String
  type SaltedPassword = String
  type Token          = String
  type SecretKey      = String

  final case class LoginForm(
    username: UserName,
    password: Password
  )

}
