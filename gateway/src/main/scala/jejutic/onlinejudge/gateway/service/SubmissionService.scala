package jejutic.onlinejudge.gateway.service

import cats.Monad
import cats.data.{EitherT, OptionT}
import cats.syntax.all.*
import jejutic.onlinejudge.gateway.domain.Problem.ProblemId
import jejutic.onlinejudge.gateway.domain.Submission.*
import jejutic.onlinejudge.gateway.domain.User.{Admin, Participant}
import jejutic.onlinejudge.gateway.domain.authorization.UserName
import jejutic.onlinejudge.gateway.domain.error.PersistenceError.NotFoundError
import jejutic.onlinejudge.gateway.domain.error.PersistenceError.NotFoundError.SubmissionNotFound
import jejutic.onlinejudge.gateway.domain.{Submission, User}
import jejutic.onlinejudge.gateway.repository.algebra.SubmissionRepository
import jejutic.onlinejudge.gateway.repository.model.SubmissionModel
import jejutic.onlinejudge.gateway.repository.model.SubmissionModel.{NewSubmissionModel, SubmissionStatus}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.syntax.*

trait SubmissionService[F[_]] {

  def submit(
    username: UserName,
    problemId: ProblemId,
    submission: NewSubmission
  ): F[Either[NotFoundError, SubmissionId]]

  def checkSubmission(user: User, id: SubmissionId): F[Option[Submission]]

  def listSubmissions(username: UserName): F[List[SubmissionId]]

  def updateStatus(
    submissionResult: SubmissionResult
  ): F[Either[SubmissionNotFound, Unit]]

}

object SubmissionService {

  def make[F[_]: Monad: Logger](
    submissionRepository: SubmissionRepository[F],
    submissionPublisher: SubmissionPublisher[F]
  ): SubmissionService[F] = new SubmissionService[F] {

    override def submit(
      username: UserName,
      problemId: ProblemId,
      submission: NewSubmission
    ): F[Either[NotFoundError, SubmissionId]] =
      (for {
        submissionId <- EitherT(
          submissionRepository.createDraft(
            NewSubmissionModel(
              problemId,
              submission.answer,
              username
            )
          )
        )
        _ <- EitherT.liftF(debug"Submission draft created: $submissionId")
        _ <- EitherT.liftF(
          submissionPublisher
            .publish(
              SubmissionEntity(
                submissionId,
                problemId,
                submission.answer
              )
            )
        )
        _ <- EitherT.liftF(debug"Submission $submissionId published to the queue")
        _ <- EitherT(
          submissionRepository
            .publishDraft(submissionId)
        )
        _ <- EitherT.liftF(debug"Submission $submissionId saved in the database")
      } yield submissionId).value

    private def modelStatusToDomain: SubmissionStatus => Submission.SubmissionStatus = {
      case SubmissionStatus.Processing  => Processing()
      case SubmissionStatus.Ok          => Ok()
      case SubmissionStatus.WrongAnswer => WrongAnswer()
    }

    private def mapModel(model: SubmissionModel): Submission =
      Submission(
        model.problemId,
        model.answer,
        model.username,
        modelStatusToDomain(model.status)
      )

    override def checkSubmission(
      user: User,
      id: SubmissionId
    ): F[Option[Submission]] =
      (for {
        submission <- OptionT(
          submissionRepository
            .find(id)
            .map(
              _.map(mapModel)
            )
        )
        _ <- OptionT.fromOption(
          Option.when(
            user match
              case Participant(username, _) =>
                submission.username == username
              case Admin(_) => true
          )(())
        )
      } yield submission).value

    override def listSubmissions(username: UserName): F[List[SubmissionId]] =
      submissionRepository
        .findAll(username)

    override def updateStatus(
      submissionResult: SubmissionResult
    ): F[Either[SubmissionNotFound, Unit]] =
      submissionRepository.updateStatus(
        submissionResult.submissionId,
        submissionResult.status match {
          case Ok() =>
            SubmissionStatus.Ok: SubmissionStatus.Ok.type | SubmissionStatus.WrongAnswer.type
          case WrongAnswer() => SubmissionStatus.WrongAnswer
        }
      )
  }

}
