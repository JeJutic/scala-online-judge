package jejutic.onlinejudge.gateway.repository.interpreter

import cats.effect.MonadCancelThrow
import cats.syntax.all.*
import doobie.ConnectionIO
import doobie.implicits.*
import doobie.util.transactor.Transactor
import jejutic.onlinejudge.gateway.domain.Problem.ProblemId
import jejutic.onlinejudge.gateway.repository.algebra.ProblemRepository
import jejutic.onlinejudge.gateway.repository.model.ProblemModel
import jejutic.onlinejudge.gateway.repository.model.ProblemModel.NewProblemModel

private object ProblemSql {

  def insert(problem: NewProblemModel): ConnectionIO[Unit] =
    sql"""
         |INSERT INTO problem (description)
         |VALUES (${problem.description})
         |""".stripMargin.update.run.void

  def lastInserted: ConnectionIO[ProblemId] =
    sql"select lastval()".query[ProblemId].unique

  def find(id: ProblemId): ConnectionIO[Option[ProblemModel]] =
    sql"""
         |SELECT id, description FROM problem
         |WHERE id = $id
         |""".stripMargin.query[ProblemModel].option

  def findAll: ConnectionIO[List[ProblemId]] =
    sql"SELECT id FROM problem".stripMargin.query[ProblemId].to[List]

}

class ProblemRepositoryInterpreter[F[_]: MonadCancelThrow](
  xa: Transactor[F]
) extends ProblemRepository[F] {

  override def add(problem: NewProblemModel): F[ProblemId] =
    (for {
      _  <- ProblemSql.insert(problem)
      id <- ProblemSql.lastInserted
    } yield id)
      .transact(xa)

  override def find(id: ProblemId): F[Option[ProblemModel]] =
    ProblemSql
      .find(id)
      .transact(xa)

  override def findAll(): F[List[ProblemId]] =
    ProblemSql.findAll
      .transact(xa)

}
