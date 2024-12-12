package jejutic.onlinejudge.gateway.repository.interpreter

import cats.effect.MonadCancelThrow
import doobie.ConnectionIO
import doobie.implicits.*
import doobie.util.transactor.Transactor
import jejutic.onlinejudge.gateway.domain.authorization.UserName
import jejutic.onlinejudge.gateway.repository.algebra.AdminRepository

private object AdminSql {

  def find(username: UserName): ConnectionIO[Option[Unit]] =
    sql"""
         |SELECT FROM admin
         |WHERE username = $username
         |""".stripMargin.query[Unit].option

}

class AdminRepositoryInterpreter[F[_]: MonadCancelThrow](
  xa: Transactor[F]
) extends AdminRepository[F] {

  override def find(username: UserName): F[Boolean] =
    AdminSql
      .find(username)
      .map(_.isDefined)
      .transact(xa)

}
