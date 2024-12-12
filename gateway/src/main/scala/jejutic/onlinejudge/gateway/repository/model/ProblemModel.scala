package jejutic.onlinejudge.gateway.repository.model

import jejutic.onlinejudge.gateway.domain.Problem.ProblemId

final case class ProblemModel(
  problemId: ProblemId,
  description: String
)

object ProblemModel {

  final case class NewProblemModel(
    description: String
  )

}
