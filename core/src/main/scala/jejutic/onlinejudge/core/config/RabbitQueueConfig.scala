package jejutic.onlinejudge.core.config

import dev.profunktor.fs2rabbit.model.{ExchangeName, QueueName, RoutingKey}

final case class RabbitQueueConfig(
  name: QueueName,
  exchangeName: ExchangeName,
  routingKey: RoutingKey
)
