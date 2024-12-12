package jejutic.onlinejudge.gateway.domain.error

sealed trait PersistenceError

object PersistenceError {

  sealed trait AlreadyExistsError extends PersistenceError

  object AlreadyExistsError {

    class UsernameAlreadyExists extends AlreadyExistsError
    class EmailAlreadyExists    extends AlreadyExistsError

  }

  sealed trait NotFoundError extends PersistenceError {
    def id: Long
  }

  object NotFoundError {

//    class UserNotFound extends NotFoundError

    final case class ProblemNotFound(override val id: Long) extends NotFoundError

    final case class SubmissionNotFound(override val id: Long) extends NotFoundError

  }

}
