package jejutic.onlinejudge.core.domain

object Submission {

  type SubmissionId = Long
  type ProblemId    = Long

  sealed trait ResultSubmissionStatus

  final case class Ok()          extends ResultSubmissionStatus
  final case class WrongAnswer() extends ResultSubmissionStatus

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
