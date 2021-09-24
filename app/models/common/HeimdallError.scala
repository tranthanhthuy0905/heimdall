package models.common

case class HeimdallError(message: String, errorCode: HeimdallError.ErrorCode) extends Throwable


object HeimdallError extends scala.AnyRef {

  sealed trait ErrorCode
  case object ErrorCode extends scala.AnyRef {
    case object VALIDATION_ERROR extends ErrorCode {
      val name: String = "validation_error"
    }
    case object INTERNAL_SERVER_ERROR extends ErrorCode {
      val name: String = "internal_server_error"
    }

    case object NOT_FOUND extends ErrorCode {
      val name: String = "not_found"
    }
  }
}

