package services.komrade

import com.evidence.service.komrade.thrift._

case class PlaybackSettings(position: WatermarkPosition) {

  def toMap: Map[String, String] =
    Map("lp" -> position.value.toString)
}

object PlaybackSettings {
  def fromThrift(watermarkSetting: WatermarkSetting) = {
    PlaybackSettings(position = watermarkSetting.position)
  }
}
