package models.common

import models.auth.AuthorizationData
import play.api.libs.typedmap.TypedKey

object AuthorizationAttr {
  val Key: TypedKey[AuthorizationData] = TypedKey.apply[AuthorizationData]("auth")
}

object MediaIdentAttr {
  val Key: TypedKey[MediaIdent] = TypedKey.apply[MediaIdent]("media")
}
