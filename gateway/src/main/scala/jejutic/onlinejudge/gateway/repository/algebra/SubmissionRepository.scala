package jejutic.onlinejudge.gateway.repository.algebra

import jejutic.onlinejudge.gateway.domain.Submission.SubmissionId
import jejutic.onlinejudge.gateway.domain.authorization.UserName
import jejutic.onlinejudge.gateway.domain.error.PersistenceError.NotFoundError.{
  ProblemNotFound,
  SubmissionNotFound
}
import jejutic.onlinejudge.gateway.repository.model.SubmissionModel
import jejutic.onlinejudge.gateway.repository.model.SubmissionModel.{
  NewSubmissionModel,
  SubmissionStatus
}

trait SubmissionRepository[F[_]] {

  def createDraft(
    submission: NewSubmissionModel
  ): F[Either[ProblemNotFound, SubmissionId]]

  def publishDraft(
    submissionId: SubmissionId
  ): F[Either[SubmissionNotFound, Unit]]

  def find(id: SubmissionId): F[Option[SubmissionModel]]

  def findAll(username: UserName): F[List[SubmissionId]]

  def updateStatus(
    submissionId: SubmissionId,
    newStatus: SubmissionStatus.Ok.type | SubmissionStatus.WrongAnswer.type
  ): F[Either[SubmissionNotFound, Unit]]

}
