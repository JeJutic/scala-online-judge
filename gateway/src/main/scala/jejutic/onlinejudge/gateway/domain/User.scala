package jejutic.onlinejudge.gateway.domain

import eu.timepit.refined.api.Refined
import eu.timepit.refined.boolean.And
import eu.timepit.refined.collection.{MaxSize, MinSize, NonEmpty}
import eu.timepit.refined.string.MatchesRegex
import jejutic.onlinejudge.gateway.domain.authorization.{Password, UserName}

enum User(val username: UserName) {

  case Participant(
    override val username: UserName,
    email: User.Email
  ) extends User(username)

  case Admin(override val username: UserName) extends User(username)

}

object User {

  type Email = String

  type UserNameRestricted = UserName Refined (NonEmpty And MaxSize[64])
  type PasswordRestricted = Password Refined (MinSize[4] And MaxSize[64])
  type EmailRestricted    = Email Refined MatchesRegex["""(\w)+@([\w\.]+)"""]

  final case class NewParticipant(
    username: UserNameRestricted,
    password: PasswordRestricted,
    email: EmailRestricted
  )

}
