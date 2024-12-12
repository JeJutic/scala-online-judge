package jejutic.onlinejudge.core

import cats.Monad
import cats.effect.kernel.{Concurrent, Temporal}
import cats.syntax.all.*
import dev.profunktor.fs2rabbit.json.Fs2JsonDecoder
import dev.profunktor.fs2rabbit.model.AckResult.{Ack, NAck}
import dev.profunktor.fs2rabbit.model.{AckResult, AmqpEnvelope, AmqpMessage, AmqpProperties}
import fs2.{Pipe, Stream}
import io.circe.Printer
import io.circe.generic.auto.*
import io.circe.syntax.*
import jejutic.onlinejudge.core.domain.Submission.{Ok, SubmissionEntity, SubmissionResult, WrongAnswer}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.syntax.*

import scala.concurrent.duration.FiniteDuration

class StubWorker[F[_]: Concurrent: Temporal: Logger](
  processingTime: FiniteDuration,
  queueProducer: AmqpMessage[String] => F[Unit],
  acker: AckResult => F[Unit],
  consumer: Stream[F, AmqpEnvelope[String]]
) {

  private object ioDecoder extends Fs2JsonDecoder
  import ioDecoder.*

  private def malformedJson: Pipe[F, io.circe.Error, Unit] = _.evalMap { error =>
    error"Malformed json consumed: $error"
  }

  private def processSubmission(submission: SubmissionEntity): F[Unit] =
    for {
      result <- Temporal[F]
        .sleep(processingTime)
        .map { _ =>
          SubmissionResult(
            submission.submissionId,
            if (submission.answer == "4")
              Ok()
            else WrongAnswer()
          )
        }
      _ <- queueProducer(
        AmqpMessage(
          result.asJson
            .printWith(Printer.noSpaces),
          AmqpProperties.empty
        )
      )
    } yield ()

  def run: F[Unit] =
    consumer
      .map(jsonDecode[SubmissionEntity])
      .flatMap {
        case (Left(error), tag) =>
          Stream
            .eval(Monad[F].pure(error))
            .through(malformedJson)
            .as(NAck(tag))
        case (Right(submission), tag) =>
          Stream
            .eval(Monad[F].pure(submission))
            .evalTap(submission => debug"Processing submission: $submission")
            .evalMap(processSubmission)
            .evalTap(_ => debug"Submission processed")
            .as(Ack(tag))
      }
      .evalMap(acker)
      .compile
      .drain

}
