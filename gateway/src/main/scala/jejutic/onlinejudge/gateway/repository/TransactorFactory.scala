package jejutic.onlinejudge.gateway.repository

import cats.effect.kernel.{Async, Resource}
import com.zaxxer.hikari.HikariConfig
import doobie.hikari.HikariTransactor
import jejutic.onlinejudge.gateway.config.DbConfig

object TransactorFactory {

  def transactor[F[_]: Async](cfg: DbConfig): Resource[F, HikariTransactor[F]] =
    for {
      hikariConfig <- Resource.pure {
        val config = HikariConfig()
        config.setDriverClassName(cfg.driver)
        config.setJdbcUrl(cfg.url)
        config.setUsername(cfg.user)
        config.setPassword(cfg.password)
        config
      }
      xa <- HikariTransactor.fromHikariConfig[F](hikariConfig)
    } yield xa

}
