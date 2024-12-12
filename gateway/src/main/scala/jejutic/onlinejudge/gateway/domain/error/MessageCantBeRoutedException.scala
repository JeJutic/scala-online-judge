package jejutic.onlinejudge.gateway.domain.error

import dev.profunktor.fs2rabbit.model.PublishReturn

class MessageCantBeRoutedException(private val pr: PublishReturn) extends RuntimeException {

  override def getMessage: String =
    s"Publish listener: $pr"

}
