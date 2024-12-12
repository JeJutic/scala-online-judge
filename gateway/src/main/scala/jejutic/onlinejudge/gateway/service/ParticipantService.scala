package jejutic.onlinejudge.gateway.service

import cats.Monad
import cats.syntax.all.*
import eu.timepit.refined.auto.autoUnwrap
import jejutic.onlinejudge.gateway.domain.User.{Email, NewParticipant}
import jejutic.onlinejudge.gateway.domain.authorization.UserName
import jejutic.onlinejudge.gateway.domain.error.PersistenceError.AlreadyExistsError
import jejutic.onlinejudge.gateway.repository.algebra.ParticipantRepository
import jejutic.onlinejudge.gateway.repository.model.ParticipantModel.NewParticipantModel
import org.typelevel.log4cats.Logger

trait ParticipantService[F[_]] {

  def addParticipant(
    participant: NewParticipant
  ): F[Either[AlreadyExistsError, Unit]]

  def getEmail(username: UserName): F[Option[Email]]

}

object ParticipantService {

  def make[F[_]: Monad: Logger](
    passwordSalter: PasswordSalter[F],
    participantRepository: ParticipantRepository[F]
  ): ParticipantService[F] = new ParticipantService[F] {

    override def addParticipant(
      participant: NewParticipant
    ): F[Either[AlreadyExistsError, Unit]] =
      for {
        saltedPassword <- passwordSalter.salt(participant.password)
        participantModel = NewParticipantModel(
          participant.username,
          participant.email
        )
        addResult <- participantRepository.add(participantModel, saltedPassword)
      } yield addResult

    override def getEmail(username: UserName): F[Option[Email]] =
      participantRepository
        .find(username)
        .map(_.map(_.email))

  }

}
