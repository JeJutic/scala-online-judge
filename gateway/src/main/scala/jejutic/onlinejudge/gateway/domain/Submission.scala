package jejutic.onlinejudge.gateway.domain

import jejutic.onlinejudge.gateway.domain.Problem.ProblemId
import jejutic.onlinejudge.gateway.domain.Submission.SubmissionStatus
import jejutic.onlinejudge.gateway.domain.authorization.UserName

final case class Submission(
  problemId: ProblemId,
  answer: String,
  username: UserName,
  status: SubmissionStatus
)

object Submission {

  type SubmissionId = Long

  sealed trait SubmissionStatus

  sealed trait ResultSubmissionStatus extends SubmissionStatus

  final case class Processing()  extends SubmissionStatus
  final case class Ok()          extends ResultSubmissionStatus
  final case class WrongAnswer() extends ResultSubmissionStatus

  final case class NewSubmission(
    answer: String
  )

  final case class SubmissionEntity(
    submissionId: SubmissionId,
    problemId: ProblemId,
    answer: String
  )

  final case class SubmissionResult(
    submissionId: SubmissionId,
    status: ResultSubmissionStatus
  )

}
