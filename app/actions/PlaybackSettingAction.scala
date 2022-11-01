package actions

import com.evidence.service.common.monad.FutureEither
import com.evidence.service.thrift.v2.RequestInfo

import javax.inject.Inject
import models.common.HeimdallRequest
import play.api.mvc.{ActionRefiner, Result, Results}
import services.komrade.{KomradeClient, PlaybackSettings}

import java.util.UUID
import scala.concurrent.{ExecutionContext, Future}

case class PlaybackSettingAction @Inject()(komrade: KomradeClient)(implicit val executionContext: ExecutionContext)
    extends ActionRefiner[HeimdallRequest, HeimdallRequest] {
  override def refine[A](input: HeimdallRequest[A]): Future[Either[Result, HeimdallRequest[A]]] = {
    for {
      watermarkSetting <- FutureEither(
        komrade
          .getWatermarkSettings(partnerId = input.audienceId, requestInfo = Some(input.requestInfo))
          .map(setting => withSomeValue(Some(setting), "failed to retrieve playback settings")))
    } yield input.copy(playbackSettings = Some(PlaybackSettings.fromThrift(watermarkSetting)))
  }.future

  private def withSomeValue[T](optionValue: Option[T], errorMessage: String): Either[Result, T] =
    optionValue.toRight(Results.InternalServerError(errorMessage))

}
