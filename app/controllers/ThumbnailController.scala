package controllers

import javax.inject.Inject
import models.auth.{AuthorizationAttr, JWTWrapper}
import models.common.{HeimdallActionBuilder, RtmQueryParams}
import models.hls.Watermark
import play.api.libs.ws.WSResponse
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import services.nino.{NinoClient, NinoClientAction}
import services.rtm.{RtmClient, RtmResponseHandler}

import scala.concurrent.{ExecutionContext, Future}

class ThumbnailController @Inject()(action: HeimdallActionBuilder,
                                    rtm: RtmClient,
                                    watermark: Watermark,
                                    nino: NinoClient,
                                    components: ControllerComponents)
                                   (implicit ex: ExecutionContext)
  extends AbstractController(components) {

  def thumbnail: Action[AnyContent] = action.async { implicit request =>
    val authHandler = request.attrs(AuthorizationAttr.Key)
    val rtmResponse = for {
      // TODO: refactor actions and move the access logic into a separate action builder.
      // TODO: this can be done after performance requirements are determined and met.
      accessResult <- nino.enforce(authHandler.jwtString, request.rtmQuery.media.toEvidenceEntityDescriptors, NinoClientAction.View)
      _ <- utils.Predicate(accessResult)(
        new Exception(s"${request.path}: media [${request.rtmQuery.media}] does not have ${NinoClientAction.View} access")
      )
      maybeUpdatedQuery <- maybeAddLabel(request.rtmQuery, authHandler.jwt)
      response <- rtm.send(maybeUpdatedQuery)
    } yield response

    val okCallback = (response: WSResponse) => {
      val contentType = response.headers.get("Content-Type").flatMap(_.headOption).getOrElse("image/jpeg")
      Ok.chunked(response.bodyAsSource).as(contentType)
    }

    rtmResponse map { response =>
      RtmResponseHandler(response, okCallback, Seq[(String, Any)](
        "path" -> request.rtmQuery.path,
        "media" -> request.rtmQuery.media,
        "status" -> response.status,
        "message" -> response.body
      ))
    }
  }

  private def maybeAddLabel(query: RtmQueryParams, jwt: JWTWrapper): Future[RtmQueryParams] = {
    /** isMulticam determines if heimdall is dealing with multicam request or not.
      * Normally thumbnails multicam requests contain `customlayout`. However, in some cases when customlayout is not
      * provided, RTM will ignore width and height values and generate thumbnails using `defaultLayout`.
      * See https://git.taservs.net/ecom/rtm/blob/6a7a6d1d2b2413c244262b49c5dfd151c4b67145/src/rtm/server/core/combine.go#L130
      * Because of the variety of use-cases, heimdall simply generates label for all multicam thumbnail requests.
      */
    if (isMulticam(query) || isResolutionHighEnough(query)) {
      watermark.augmentQuery(query, jwt)
    } else {
      Future.successful(query)
    }
  }

  private def isResolutionHighEnough(query: RtmQueryParams): Boolean = {
    isAboveThreshold(query, "width", 400 ) &&
      isAboveThreshold(query, "height", 200 )
  }

  /**
    * isAboveThreshold checks if a certain parameter value is equal to or above a threshold.
    *
    *  @param query query map
    *  @param paramName name of a parameter that will be validated.
    *  @param threshold empirically chosen threshold value.
    *  @return Boolean value indicating if validated parameter presents in the query and equal to or
    *          greater than the threshold.
    */
  private def isAboveThreshold(query: RtmQueryParams, paramName: String, threshold: Int): Boolean = {
    query.params.getOrElse(paramName, "0").toInt >= threshold
  }

  private def isMulticam(query: RtmQueryParams): Boolean = {
    query.media.evidenceIds.length > 1
  }

}
