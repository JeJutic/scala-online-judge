package jejutic.onlinejudge.gateway.service

import cats.Monad
import cats.effect.kernel.Concurrent
import dev.profunktor.fs2rabbit.json.Fs2JsonDecoder
import dev.profunktor.fs2rabbit.model.AckResult.{Ack, NAck}
import dev.profunktor.fs2rabbit.model.StreamAckerConsumer
import fs2.{Pipe, Stream}
import io.circe.generic.auto.*
import jejutic.onlinejudge.gateway.domain.Submission.SubmissionResult
import jejutic.onlinejudge.gateway.domain.error.PersistenceError.NotFoundError.SubmissionNotFound
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.syntax.*

trait SubmissionResultProcessor[F[_]] {
  def process(ackerConsumer: StreamAckerConsumer[F, String]): F[Unit]
}

object SubmissionResultProcessor {

  def make[F[_]: Concurrent: Logger](
    submissionService: SubmissionService[F]
  ): SubmissionResultProcessor[F] = new SubmissionResultProcessor[F] {

    object ioDecoder extends Fs2JsonDecoder
    import ioDecoder.*

    private def malformedJson: Pipe[F, io.circe.Error, Unit] = _.evalMap { error =>
      error"Malformed json consumed: $error"
    }

    override def process(ackerConsumer: StreamAckerConsumer[F, String]): F[Unit] = {
      val (acker, consumer) = ackerConsumer
      consumer
        .map(jsonDecode[SubmissionResult])
        .flatMap {
          case (Left(error), tag) =>
            Stream
              .eval(Monad[F].pure(error))
              .through(malformedJson)
              .as(NAck(tag))
          case (Right(result), tag) =>
            Stream
              .eval(Monad[F].pure(result))
              .evalTap(result => debug"Processing submission result: $result")
              .evalMap(submissionService.updateStatus)
              .evalMap {
                case _: Left[SubmissionNotFound, Unit] =>
                  warn"Submission not found"
                case Right(()) =>
                  debug"Submission status updated successfully"
              }
              .as(Ack(tag))
        }
        .evalMap(acker)
        .compile
        .drain
    }
  }

}
