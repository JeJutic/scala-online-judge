package jejutic.onlinejudge.gateway.repository.algebra

import jejutic.onlinejudge.gateway.domain.authorization.{SaltedPassword, UserName}
import jejutic.onlinejudge.gateway.domain.error.PersistenceError.AlreadyExistsError
import jejutic.onlinejudge.gateway.repository.model.ParticipantModel
import jejutic.onlinejudge.gateway.repository.model.ParticipantModel.NewParticipantModel

trait ParticipantRepository[F[_]] {

  def add(
    participant: NewParticipantModel,
    password: SaltedPassword
  ): F[Either[AlreadyExistsError, Unit]]

  def find(username: UserName): F[Option[ParticipantModel]]

}
