package jejutic.onlinejudge.gateway.repository.interpreter

import cats.data.EitherT
import cats.effect.MonadCancelThrow
import cats.syntax.all.*
import doobie.ConnectionIO
import doobie.implicits.*
import doobie.postgres.sqlstate.class23.UNIQUE_VIOLATION
import doobie.util.transactor.Transactor
import jejutic.onlinejudge.gateway.domain.authorization.{SaltedPassword, UserName}
import jejutic.onlinejudge.gateway.domain.error.PersistenceError.AlreadyExistsError
import jejutic.onlinejudge.gateway.domain.error.PersistenceError.AlreadyExistsError.EmailAlreadyExists
import jejutic.onlinejudge.gateway.repository.algebra.ParticipantRepository
import jejutic.onlinejudge.gateway.repository.model.ParticipantModel.NewParticipantModel
import jejutic.onlinejudge.gateway.repository.model.{ParticipantModel, UserModel}

private object ParticipantSql {

  // we need E here because doobie.ConnectionIO isn't covariant
  def insert[E >: EmailAlreadyExists](
    participant: NewParticipantModel
  ): ConnectionIO[Either[E, Unit]] =
    sql"""
         |INSERT INTO participant (username, email)
         |VALUES (${participant.username}, ${participant.email})
         |""".stripMargin.update.run.void
      .attemptSomeSqlState { case UNIQUE_VIOLATION =>
        EmailAlreadyExists()
      }

  def find(username: UserName): ConnectionIO[Option[ParticipantModel]] =
    sql"""
         |SELECT username, email FROM participant
         |WHERE username = $username
         |""".stripMargin.query[ParticipantModel].option

}

class ParticipantRepositoryInterpreter[F[_]: MonadCancelThrow](
  xa: Transactor[F]
) extends ParticipantRepository[F] {

  override def add(
    participant: NewParticipantModel,
    password: SaltedPassword
  ): F[Either[AlreadyExistsError, Unit]] =
    (for { // one transaction
      _ <- EitherT(
        UserSql.insert(
          UserModel(participant.username, password)
        )
      )
      _ <- EitherT(ParticipantSql.insert(participant))
    } yield ()).value
      .transact(xa)

  override def find(username: UserName): F[Option[ParticipantModel]] =
    ParticipantSql
      .find(username)
      .transact(xa)

}
