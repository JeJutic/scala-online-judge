package jejutic.onlinejudge.core.config

import cats.effect.kernel.Sync
import pureconfig.generic.ProductHint
import pureconfig.generic.semiauto.deriveReader
import pureconfig.{ConfigReader, ConfigSource}

import scala.concurrent.duration.FiniteDuration

final case class ApplicationConfig(
  processingTime: FiniteDuration,
  rabbitClient: RabbitClientConfig
)

object ApplicationConfig {

  private given [T]: ProductHint[T] = ProductHint[T](allowUnknownKeys = false)

  private given ConfigReader[ApplicationConfig] = deriveReader

  def load[F[_]: Sync](
    config: ConfigSource = ConfigSource.resources("application.conf")
  ): F[Either[String, ApplicationConfig]] =
    Sync[F].delay(
      config
        .load[ApplicationConfig]()
        .left
        .map(_.prettyPrint())
    )

}
