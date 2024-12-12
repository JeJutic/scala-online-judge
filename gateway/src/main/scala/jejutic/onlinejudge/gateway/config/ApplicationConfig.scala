package jejutic.onlinejudge.gateway.config

import cats.effect.kernel.Sync
import pureconfig.generic.ProductHint
import pureconfig.generic.semiauto.deriveReader
import pureconfig.{ConfigReader, ConfigSource}

final case class ApplicationConfig(
  server: ServerConfig,
  database: DbConfig,
  auth: AuthConfig,
  swaggerClient: SwaggerClientConfig,
  rabbitClient: RabbitClientConfig
)

object ApplicationConfig {

  import ServerConfig.given

  private given [T]: ProductHint[T] = ProductHint[T](allowUnknownKeys = false)

  private given ConfigReader[ApplicationConfig] = deriveReader

  // not a typed error bc it's close to "end of the world"
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
