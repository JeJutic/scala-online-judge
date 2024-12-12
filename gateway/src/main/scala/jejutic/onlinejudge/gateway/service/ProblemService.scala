package jejutic.onlinejudge.gateway.service

import cats.Monad
import cats.syntax.all.*
import jejutic.onlinejudge.gateway.domain.Problem
import jejutic.onlinejudge.gateway.domain.Problem.ProblemId
import jejutic.onlinejudge.gateway.repository.algebra.ProblemRepository
import jejutic.onlinejudge.gateway.repository.model.ProblemModel.NewProblemModel
import org.typelevel.log4cats.Logger

trait ProblemService[F[_]] {

  def createProblem(problem: Problem): F[ProblemId]

  def findProblem(id: ProblemId): F[Option[Problem]]

  def findAll(): F[List[ProblemId]]

}

object ProblemService {

  def make[F[_]: Monad: Logger](
    problemRepository: ProblemRepository[F]
  ): ProblemService[F] = new ProblemService[F] {

    override def createProblem(problem: Problem): F[ProblemId] =
      problemRepository.add(
        NewProblemModel(problem.description)
      )

    override def findProblem(id: ProblemId): F[Option[Problem]] =
      problemRepository
        .find(id)
        .map(_.map(model => Problem(model.description)))

    override def findAll(): F[List[ProblemId]] =
      problemRepository
        .findAll()

  }

}
