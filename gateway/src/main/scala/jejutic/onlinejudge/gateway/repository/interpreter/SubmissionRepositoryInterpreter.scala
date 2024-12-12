package jejutic.onlinejudge.gateway.repository.interpreter

import cats.data.EitherT
import cats.effect.MonadCancelThrow
import cats.syntax.all.*
import doobie.ConnectionIO
import doobie.implicits.*
import doobie.postgres.sqlstate.class23.FOREIGN_KEY_VIOLATION
import doobie.util.meta.Meta
import doobie.util.transactor.Transactor
import jejutic.onlinejudge.gateway.domain.Submission.SubmissionId
import jejutic.onlinejudge.gateway.domain.authorization.UserName
import jejutic.onlinejudge.gateway.domain.error.PersistenceError.NotFoundError.{
  ProblemNotFound,
  SubmissionNotFound
}
import jejutic.onlinejudge.gateway.repository.algebra.SubmissionRepository
import jejutic.onlinejudge.gateway.repository.model.SubmissionModel
import jejutic.onlinejudge.gateway.repository.model.SubmissionModel.{
  NewSubmissionModel,
  SubmissionStatus
}

private object SubmissionSql {

  given Meta[SubmissionStatus] = Meta[String].timap { statusName =>
    SubmissionStatus
      .fromName(statusName)
      .get // we shouldn't care about data schema error on the service layer
  }(_.name)

  def insertDraft(
    submission: NewSubmissionModel
  ): ConnectionIO[Either[ProblemNotFound, Unit]] =
    sql"""
         |INSERT INTO submission_draft (answer, username, problem_id)
         |VALUES (${submission.answer}, ${submission.username},
         |  ${submission.problemId})
         |""".stripMargin.update.run.void
      .attemptSomeSqlState { case FOREIGN_KEY_VIOLATION =>
        ProblemNotFound(submission.problemId)
      }

  def insert(
    submissionId: SubmissionId
  ): ConnectionIO[Either[SubmissionNotFound, Unit]] =
    sql"""
         |INSERT INTO submission (id, status_id)
         |VALUES ($submissionId,
         |  (SELECT id FROM submission_status
         |  WHERE name = ${SubmissionStatus.Processing})
         |)""".stripMargin.update.run.void
      .attemptSomeSqlState { case FOREIGN_KEY_VIOLATION =>
        SubmissionNotFound(submissionId)
      }

  def lastInserted: ConnectionIO[SubmissionId] =
    sql"select lastval()".query[SubmissionId].unique

  def find(id: SubmissionId): ConnectionIO[Option[SubmissionModel]] =
    sql"""
         |SELECT submission.id, problem_id, answer, username,
         |submission_status.name FROM submission
         |INNER JOIN submission_draft ON submission.id = submission_draft.id
         |INNER JOIN submission_status ON status_id = submission_status.id
         |WHERE submission.id = $id
         |""".stripMargin.query[SubmissionModel].option

  def findAll(username: UserName): ConnectionIO[List[SubmissionId]] =
    sql"""
         |SELECT id FROM submission_draft
         |WHERE username = $username
         |""".stripMargin.query[SubmissionId].to[List]

  def updateStatus(
    id: SubmissionId,
    newStatus: SubmissionStatus
  ): ConnectionIO[Int] =
    sql"""
         |UPDATE submission
         |SET status_id =
         |(SELECT id FROM submission_status WHERE name = $newStatus)
         |WHERE submission.id = $id
         |""".stripMargin.update.run

}

class SubmissionRepositoryInterpreter[F[_]: MonadCancelThrow](
  xa: Transactor[F]
) extends SubmissionRepository[F] {

  override def createDraft(
    submission: NewSubmissionModel
  ): F[Either[ProblemNotFound, SubmissionId]] =
    (for {
      _ <- EitherT(SubmissionSql.insertDraft(submission))
      id <- EitherT.liftF(
        SubmissionSql.lastInserted
      )
    } yield id).value
      .transact(xa)

  override def publishDraft(
    submissionId: SubmissionId
  ): F[Either[SubmissionNotFound, Unit]] =
    SubmissionSql
      .insert(submissionId)
      .transact(xa)

  override def find(id: SubmissionId): F[Option[SubmissionModel]] =
    SubmissionSql
      .find(id)
      .transact(xa)

  override def findAll(username: UserName): F[List[SubmissionId]] =
    SubmissionSql
      .findAll(username)
      .transact(xa)

  override def updateStatus(
    submissionId: SubmissionId,
    newStatus: SubmissionStatus.Ok.type | SubmissionStatus.WrongAnswer.type
  ): F[Either[SubmissionNotFound, Unit]] =
    SubmissionSql
      .updateStatus(submissionId, newStatus)
      .transact(xa)
      .map {
        case 0 => Left(SubmissionNotFound(submissionId))
        case _ => Right(())
      }

}
