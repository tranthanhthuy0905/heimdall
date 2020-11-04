package services.xpreport.playback

import play.api.libs.json.{JsPath, Reads}
import play.api.libs.functional.syntax._

// Common
case class Token(value: Option[String])

object Token extends PlaybackJsonFields {
  implicit val token: Reads[Token] = (JsPath \ tokenField).readNullable[String].map(Token(_))
}

case class Time(value: Option[String])

object Time extends PlaybackJsonFields {
  implicit val token: Reads[Time] = (JsPath \ eventTimeField).readNullable[String].map(Time(_))
}

case class BrowserName(value: Option[String])

object BrowserName extends PlaybackJsonFields {
  implicit val browserName: Reads[BrowserName] = (JsPath \ browserNameField).readNullable[String].map(BrowserName(_))
}

case class FileExtension(value: Option[String])

object FileExtension extends PlaybackJsonFields {
  implicit val fileExtension: Reads[FileExtension] =
    (JsPath \ fileExtensionField).readNullable[String].map(FileExtension(_))
}

case class TranscodedVideo(value: Option[Boolean])

object TranscodedVideo extends PlaybackJsonFields {
  implicit val transcodedVideo: Reads[TranscodedVideo] =
    (JsPath \ transcodedVideoField).readNullable[Boolean].map(TranscodedVideo(_))
}

// ------ Info data ------
//{
//  time: String
//  token: String
//  data: {
//    browserName: string
//    fileExtension: string
//    transcodedVideo: boolean
//    aggregation: {
//      360p:  {
//          - totalViewDuration: number
//          - totalInputDelay: number,
//          - totalBufferingTime: number,
//      },
//      480p: {
//          - totalViewDuration: number
//          - totalInputDelay: number,
//          - totalBufferingTime: number,
//      },
//      720p: { ... similar to 360p },
//      1080p: { ... similar to 360p },
//      2K: { ... similar to 360p },
//      4K: { ... similar to 360p }
//    }
//  }
//}
case class EventsInfo(
  time: Option[Time],
  token: Option[Token],
  data: Option[Data],
)

case class Data(
  browserName: Option[BrowserName],
  fileExtension: Option[FileExtension],
  transcodedVideo: Option[TranscodedVideo],
  aggregationEvents: AggregationEvents,
)

object Data extends PlaybackJsonFields {
  implicit val data: Reads[Data] = (
    (JsPath \ dataField).readNullable[BrowserName] and
      (JsPath \ dataField).readNullable[FileExtension] and
      (JsPath \ dataField).readNullable[TranscodedVideo] and
      (JsPath \ dataField).read[AggregationEvents]
  )(Data.apply _)
}

case class AggregationEvents(data: Map[String, Duration])

object AggregationEvents extends PlaybackJsonFields {
  implicit val data: Reads[AggregationEvents] =
    (JsPath \ aggregationEventsField).read[Map[String, Duration]].map(AggregationEvents.apply)
}

case class Duration(
  totalViewDuration: Option[Double],
  totalInputDelay: Option[Double],
  totalBufferingTime: Option[Double],
)

object Duration extends PlaybackJsonFields {
  implicit val info: Reads[Duration] = (
    (JsPath \ totalViewDurationField).readNullable[Double] and
      (JsPath \ totalInputDelayField).readNullable[Double] and
      (JsPath \ totalBufferingTimeField).readNullable[Double]
  )(Duration.apply _)
}

// ------ Stalled data ------
//{
//  time: String
//  token: String
//  event: String
//  data: {
//    browserName: string
//    fileExtension: string
//    transcodedVideo: boolean
//    buffering: {
//       currentResolution: string,
//       duration: Number
//       reason: String // Can be buffering/seek/resChange/srcChange
//    }
//  }
//}
case class Event(value: Option[String])

object Event extends PlaybackJsonFields {
  implicit val token: Reads[Event] = (JsPath \ eventField).readNullable[String].map(Event.apply)
}

case class StalledInfo(
  time: Option[Time],
  token: Option[Token],
  event: Option[Event],
  data: Option[StalledData],
)

case class StalledData(
  browserName: Option[BrowserName],
  fileExtension: Option[FileExtension],
  transcodedVideo: Option[TranscodedVideo],
  buffering: Buffering
)

object StalledData extends PlaybackJsonFields {
  implicit val data: Reads[StalledData] = (
    (JsPath \ dataField).readNullable[BrowserName] and
      (JsPath \ dataField).readNullable[FileExtension] and
      (JsPath \ dataField).readNullable[TranscodedVideo] and
      (JsPath \ dataField).read[Buffering]
  )(StalledData.apply _)
}

case class Buffering(
  currentResolution: Option[CurrentResolution],
  stalledDuration: Option[StalledDuration],
  stalledReason: Option[StalledReason])

object Buffering extends PlaybackJsonFields {
  implicit val buffering: Reads[Buffering] = (
    (JsPath \ bufferingField).readNullable[CurrentResolution] and
      (JsPath \ bufferingField).readNullable[StalledDuration] and
      (JsPath \ bufferingField).readNullable[StalledReason]
  )(Buffering.apply _)
}

case class CurrentResolution(value: Option[Int])

object CurrentResolution extends PlaybackJsonFields {
  implicit val currentResolution: Reads[CurrentResolution] =
    (JsPath \ currentResolutionField).readNullable[Int].map(CurrentResolution.apply)
}

case class StalledDuration(value: Option[Double])

object StalledDuration extends PlaybackJsonFields {
  implicit val stalledDuration: Reads[StalledDuration] =
    (JsPath \ stalledDurationField).readNullable[Double].map(StalledDuration.apply)
}

case class StalledReason(value: Option[String])

object StalledReason extends PlaybackJsonFields {
  implicit val stalledReason: Reads[StalledReason] =
    (JsPath \ stalledReasonField).readNullable[String].map(StalledReason.apply)
}
