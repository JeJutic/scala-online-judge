package jejutic.onlinejudge.gateway.domain

final case class Problem(
  description: String
)

object Problem {
  type ProblemId = Long
}