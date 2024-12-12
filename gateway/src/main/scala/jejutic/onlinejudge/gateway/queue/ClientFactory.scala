package jejutic.onlinejudge.gateway.queue

import cats.data.Kleisli
import cats.effect.kernel.{Async, Resource}
import cats.syntax.all.*
import dev.profunktor.fs2rabbit.config.Fs2RabbitConfig
import dev.profunktor.fs2rabbit.config.declaration.DeclarationQueueConfig
import dev.profunktor.fs2rabbit.effects.{EnvelopeDecoder, MessageEncoder}
import dev.profunktor.fs2rabbit.interpreter.RabbitClient
import dev.profunktor.fs2rabbit.model.*
import dev.profunktor.fs2rabbit.model.ExchangeType.Direct
import jejutic.onlinejudge.gateway.config.{RabbitClientConfig, RabbitQueueConfig}
import jejutic.onlinejudge.gateway.domain.error.MessageCantBeRoutedException
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.syntax.*

import java.nio.charset.StandardCharsets

class ClientFactory[F[_]: Async: Logger](cfg: RabbitClientConfig) {

  private val clientResource: Resource[F, RabbitClient[F]] = {
    val fs2Config = Fs2RabbitConfig(
      host = cfg.host,
      port = cfg.port,
      virtualHost = "/",
      connectionTimeout = cfg.connectionTimeout,
      ssl = false,
      username = cfg.username,
      password = cfg.password,
      requeueOnNack = cfg.requeueOnNack,
      requeueOnReject = cfg.requeueOnReject,
      internalQueueSize = cfg.internalQueueSize
    )
    RabbitClient
      .default(fs2Config)
      .resource
  }

  private def declareQueue(
    client: RabbitClient[F],
    cfg: RabbitQueueConfig
  )(using
    channel: AMQPChannel
  ): F[Unit] =
    for {
      _ <- client.declareQueue(DeclarationQueueConfig.default(cfg.name))
      _ <- client.declareExchange(cfg.exchangeName, Direct)
      _ <- client.bindQueue(cfg.name, cfg.exchangeName, cfg.routingKey)
    } yield ()

  private val publishingListener: PublishReturn => F[Unit] = pr =>
    for {
      _ <- error"Publish listener: $pr"
      _ <- Async[F].raiseError(MessageCantBeRoutedException(pr))
    } yield ()

  type Producer = AmqpMessage[String] => F[Unit]

  private given MessageEncoder[F, AmqpMessage[String]] =
    Kleisli[F, AmqpMessage[String], AmqpMessage[Array[Byte]]] { s =>
      s
        .copy(payload = s.payload.getBytes(StandardCharsets.UTF_8))
        .pure[F]
    }

  val producerConsumer: Resource[F, (Producer, StreamAckerConsumer[F, String])] =
    for {
      client <- clientResource
      _      <- Resource.eval(info"Opening connection channel to RabbitMQ...")
      // could use `given channel` from `better-monadic-for`
      channel <- client.createConnectionChannel
      _       <- Resource.eval(info"Connection channel created")
      _ <- Resource.eval(
        declareQueue(client, cfg.answerQueue)(using
          channel
        )
      )
      _ <- Resource.eval(info"Answer queue declared")
      _ <- Resource.eval(
        declareQueue(client, cfg.resultQueue)(using
          channel
        )
      )
      _ <- Resource.eval(info"Result queue declared")
      publisher <- Resource.eval(
        client.createPublisherWithListener[AmqpMessage[String]](
          cfg.answerQueue.exchangeName,
          cfg.answerQueue.routingKey,
          PublishingFlag(mandatory = true),
          publishingListener
        )(using
          channel,
          summon[MessageEncoder[F, AmqpMessage[String]]]
        )
      )
      _ <- Resource.eval(info"Publisher created")
      ackerConsumer <- Resource.eval(
        client.createAckerConsumer(cfg.resultQueue.name)(using
          channel,
          summon[EnvelopeDecoder[F, String]]
        )
      )
      _ <- Resource.eval(info"Consumer created")
    } yield (publisher, ackerConsumer)

}
