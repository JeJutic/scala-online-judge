package jejutic.onlinejudge.gateway.config

final case class DbConfig(
  driver: String,
  url: String,
  user: String,
  password: String,
  migrationsTable: String,
  migrationsLocations: List[String]
)
