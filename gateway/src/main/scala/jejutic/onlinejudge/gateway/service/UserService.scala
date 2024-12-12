package jejutic.onlinejudge.gateway.service

import cats.Monad
import cats.data.EitherT
import cats.syntax.all.*
import jejutic.onlinejudge.gateway.domain.User
import jejutic.onlinejudge.gateway.domain.User.{Admin, Participant}
import jejutic.onlinejudge.gateway.domain.authorization.{LoginForm, Token, UserName}
import jejutic.onlinejudge.gateway.domain.error.AuthError
import jejutic.onlinejudge.gateway.domain.error.AuthError.IncorrectUsernameOrPassword
import jejutic.onlinejudge.gateway.repository.algebra.{
  AdminRepository,
  ParticipantRepository,
  UserRepository
}
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.syntax.*

trait UserService[F[_]] {

  def retrieve(username: UserName): F[Option[User]]

  def logIn(loginForm: LoginForm): F[Either[AuthError, Token]]

}

object UserService {

  def make[F[_]: Monad: Logger](
    jwtCoder: JwtCoder[F],
    passwordSalter: PasswordSalter[F],
    userRepository: UserRepository[F],
    adminRepository: AdminRepository[F],
    participantRepository: ParticipantRepository[F]
  ): UserService[F] = new UserService[F] {

    override def retrieve(username: UserName): F[Option[User]] =
      (for {
        _ <- EitherT[F, User, Unit](
          participantRepository
            .find(username)
            .map(
              _.map(model => Participant(model.username, model.email))
                .toLeft(())
            )
        )
        _ <- EitherT(
          adminRepository
            .find(username)
            .map(isAdmin => Either.cond(!isAdmin, (), Admin(username)))
        )
      } yield ()).value
        .map(_.left.toOption)

    private def verifyLogin(loginForm: LoginForm): F[Boolean] =
      for {
        passwordOption <- userRepository.find(loginForm.username)
        verified <- passwordOption.fold(
          for {
            _ <-
              warn"Couldn't login: user with username ${loginForm.username} not found"
            verificationFailed <- Monad[F].pure(false)
          } yield verificationFailed
        ) { saltedPassword =>
          for {
            verificationRes <- passwordSalter.verify(
              loginForm.password,
              saltedPassword
            )
            _ <-
              if (!verificationRes) // don't log the password
                warn"Password for user ${loginForm.username} is incorrect"
              else Monad[F].unit
          } yield verificationRes
        }
      } yield verified

    override def logIn(loginForm: LoginForm): F[Either[AuthError, Token]] =
      for {
        loginVerified <- verifyLogin(loginForm)
        eitherToken <-
          if (loginVerified)
            jwtCoder
              .encode(loginForm.username)
              .map(Right(_))
          else Monad[F].pure(Left(IncorrectUsernameOrPassword))
      } yield eitherToken

  }

}
