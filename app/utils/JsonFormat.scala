package utils

import com.evidence.service.common.logging.LazyLogging
import play.api.libs.json.{JsNull, JsObject, JsString, JsValue}

trait JsonFormat extends LazyLogging {
  protected def removeNullValues(data: JsValue): JsObject = {
    def withValue(v: JsValue): Boolean = v match {
      case JsNull       => false
      case JsString("") => false
      case _            => true
    }

    def recurse: PartialFunction[(String, JsValue), (String, JsValue)] = {
      case (key: String, value: JsValue) if withValue(value) => (key, value)
    }

    data
      .asOpt[JsObject]
      .map(_.fields.collect(recurse))
      .map(JsObject)
      .getOrElse(JsObject.empty)
  }
}
