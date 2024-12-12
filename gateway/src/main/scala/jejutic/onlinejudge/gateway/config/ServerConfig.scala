package jejutic.onlinejudge.gateway.config

import com.comcast.ip4s.{Host, Port}
import pureconfig.{ConfigReader, ConvertHelpers}

final case class ServerConfig(
  host: Host,
  port: Port
)

object ServerConfig {

  given ConfigReader[Host] = ConfigReader.fromString(
    ConvertHelpers.optF(Host.fromString)
  )

  given ConfigReader[Port] = ConfigReader.fromString(
    ConvertHelpers.optF(Port.fromString)
  )

}
