package jejutic.onlinejudge.gateway.repository.model

import jejutic.onlinejudge.gateway.domain.Problem.ProblemId
import jejutic.onlinejudge.gateway.domain.Submission.SubmissionId
import jejutic.onlinejudge.gateway.domain.authorization.UserName
import jejutic.onlinejudge.gateway.repository.model.SubmissionModel.SubmissionStatus

final case class SubmissionModel(
  submissionId: SubmissionId,
  problemId: ProblemId,
  answer: String,
  username: UserName,
  status: SubmissionStatus
)

object SubmissionModel {

  enum SubmissionStatus(val name: String) {

    case Processing  extends SubmissionStatus("processing")
    case Ok          extends SubmissionStatus("ok")
    case WrongAnswer extends SubmissionStatus("wrong answer")

  }

  object SubmissionStatus {

    def fromName(name: String): Option[SubmissionStatus] =
      SubmissionStatus.values.find(_.name == name)

  }

  final case class NewSubmissionModel(
    problemId: ProblemId,
    answer: String,
    username: UserName
  )

}
