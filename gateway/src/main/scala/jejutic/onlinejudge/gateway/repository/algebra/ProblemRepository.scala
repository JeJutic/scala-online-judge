package jejutic.onlinejudge.gateway.repository.algebra

import jejutic.onlinejudge.gateway.domain.Problem.ProblemId
import jejutic.onlinejudge.gateway.repository.model.ProblemModel
import jejutic.onlinejudge.gateway.repository.model.ProblemModel.NewProblemModel

trait ProblemRepository[F[_]] {

  def add(problem: NewProblemModel): F[ProblemId]

  def find(id: ProblemId): F[Option[ProblemModel]]

  def findAll(): F[List[ProblemId]]

}
