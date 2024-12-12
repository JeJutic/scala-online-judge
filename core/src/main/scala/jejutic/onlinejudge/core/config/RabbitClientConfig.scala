package jejutic.onlinejudge.core.config

import scala.concurrent.duration.FiniteDuration

final case class RabbitClientConfig(
  host: String,
  port: Int,
  connectionTimeout: FiniteDuration,
  username: Option[String],
  password: Option[String],
  requeueOnNack: Boolean,
  requeueOnReject: Boolean,
  internalQueueSize: Option[Int],
  answerQueue: RabbitQueueConfig,
  resultQueue: RabbitQueueConfig
)
