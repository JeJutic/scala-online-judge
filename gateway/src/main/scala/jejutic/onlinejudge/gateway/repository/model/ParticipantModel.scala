package jejutic.onlinejudge.gateway.repository.model

import jejutic.onlinejudge.gateway.domain.User.Email
import jejutic.onlinejudge.gateway.domain.authorization.UserName

final case class ParticipantModel(
  username: UserName,
  email: Email
)

object ParticipantModel {

  final case class NewParticipantModel(
    username: UserName,
    email: Email
  )

}
