package models.auth

sealed trait AuthorizationError {
  val msg: String
}

case class BackendServiceError(msg: String) extends AuthorizationError
case class TokenParseError(msg: String) extends AuthorizationError
