package jejutic.onlinejudge.gateway.config

// we could add idle-timeout for client but no need for one html page
final case class SwaggerClientConfig(
  host: String,
  port: Int
)
