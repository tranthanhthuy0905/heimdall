package models.common

import com.evidence.service.common.Convert
import com.evidence.service.common.logging.LazyLogging
import java.util.UUID

import services.rtm.HeimdallRoutes

case class RtiQueryHelper(image: FileIdent, path: String, params: Map[String, String])

object RtiQueryHelper extends LazyLogging with HeimdallRoutes {
  private final val imageThumbnailWhitelistedParams = List(
    "size_id",
    "watermark"
  )

  private final val imageThumbnail = "/media/images/view"

  private final val heimdallToRtiRoutes = Map(
    imageThumbnail -> ImageRoute("/media/images/thumbnail", imageThumbnailWhitelistedParams),
  )

  def apply(route: String, query: Map[String, Seq[String]]): Option[RtiQueryHelper] = {
    for {
      fileId <- getUuidValue(query, "file_id")
      evidenceId <- getUuidValue(query, "evidence_id")
      partnerId <- getUuidValue(query, "partner_id")
      query <- filterQueryForRoute(route, query)
    } yield RtiQueryHelper(FileIdent(fileId, evidenceId, partnerId), query.rtiPath, query.params)
  }

  private def filterQueryForRoute(route: String, query: Map[String, Seq[String]]): Option[QueryWithPath] = {
    if (route startsWith imageThumbnail) {
      filterAllowedParams(query, heimdallToRtiRoutes(imageThumbnail))
    } else {
      logger.error("unexpectedQueryRoute")("route" -> route)
      None
    }
  }

  private def filterAllowedParams(query: Map[String, Seq[String]], imageRoute: ImageRoute): Option[QueryWithPath] = {
    val filteredParams = query
      .filterKeys(imageRoute.whitelistedParams.contains(_))
      .filter({ case (_, v) => v.nonEmpty })
      .map { case (k, v) => k -> v.headOption.getOrElse("") }

    val result = QueryWithPath(imageRoute.rtiPath, filteredParams)
    if (query.keySet.size != (3 + filteredParams.keySet.size)) {
      logger.info("filteredOutRequestParameters")(
        "originalQuery" -> query,
        "filteredQuery" -> filteredParams
      )
    }
    Some(result)
  }

  private def getUuidValue(query: Map[String, Seq[String]], key: String): Option[UUID] = {
    for {
      seq <- query.get(key)
      value <- seq.headOption
      uuid <- Convert.tryToUuid(value)
    } yield uuid
  }

  case class ImageRoute(rtiPath: String, whitelistedParams: List[String])

  case class QueryWithPath(rtiPath: String, params: Map[String, String])

}