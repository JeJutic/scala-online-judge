package jejutic.onlinejudge.gateway.repository.interpreter

import cats.effect.MonadCancelThrow
import cats.syntax.all.*
import doobie.ConnectionIO
import doobie.implicits.*
import doobie.postgres.sqlstate.class23.UNIQUE_VIOLATION
import doobie.util.transactor.Transactor
import jejutic.onlinejudge.gateway.domain.authorization.{SaltedPassword, UserName}
import jejutic.onlinejudge.gateway.domain.error.PersistenceError.AlreadyExistsError.UsernameAlreadyExists
import jejutic.onlinejudge.gateway.repository.algebra.UserRepository
import jejutic.onlinejudge.gateway.repository.model.UserModel

private[interpreter] object UserSql {

  def insert[E >: UsernameAlreadyExists](user: UserModel): ConnectionIO[Either[E, Unit]] =
    sql"""
         |INSERT INTO users (username, password)
         |VALUES (${user.name}, ${user.saltedPassword})
         |""".stripMargin.update.run.void
      .attemptSomeSqlState { case UNIQUE_VIOLATION =>
        UsernameAlreadyExists()
      }

  def find(username: UserName): ConnectionIO[Option[SaltedPassword]] =
    sql"""
         |SELECT password FROM users
         |WHERE username = $username
         |""".stripMargin.query[SaltedPassword].option

}

class UserRepositoryInterpreter[F[_]: MonadCancelThrow](
  xa: Transactor[F]
) extends UserRepository[F] {

  override def find(username: UserName): F[Option[SaltedPassword]] =
    UserSql
      .find(username)
      .transact(xa)

}
