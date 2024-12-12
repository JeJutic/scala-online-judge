package jejutic.onlinejudge.gateway.service

import dev.profunktor.fs2rabbit.model.{AmqpMessage, AmqpProperties}
import io.circe.Printer
import io.circe.generic.auto.*
import io.circe.syntax.EncoderOps
import jejutic.onlinejudge.gateway.domain.Submission.SubmissionEntity

trait SubmissionPublisher[F[_]] {
  def publish(submission: SubmissionEntity): F[Unit]
}

object SubmissionPublisher {

  def make[F[_]](
    queueProducer: AmqpMessage[String] => F[Unit]
  ): SubmissionPublisher[F] = new SubmissionPublisher[F] {

    override def publish(submission: SubmissionEntity): F[Unit] =
      queueProducer(
        AmqpMessage(
          submission.asJson
            .printWith(Printer.noSpaces),
          AmqpProperties.empty
        )
      )

  }

}
