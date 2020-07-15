package services.xpreport.playback

import play.api.libs.json.{JsPath, Reads}
import play.api.libs.functional.syntax._

// Common
case class Token(value: Option[String])
object Token extends PlaybackJsonFields {
  implicit val token: Reads[Token] = (JsPath \ tokenField).readNullable[String].map(Token.apply)
}

case class Time(value: Option[String])
object Time extends PlaybackJsonFields {
  implicit val token: Reads[Time] = (JsPath \ eventTimeField).readNullable[String].map(Time.apply)
}

// ------ Info data ------
//{
//  time: String
//  token: String
//  data: {
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

case class Data(aggregationEvents: AggregationEvents)
object Data extends PlaybackJsonFields {
  implicit val data: Reads[Data] =
    (JsPath \ dataField).read[AggregationEvents].map(Data.apply)
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
//  data: {
//    buffering: {
//       currentResolution: string,
//    }
//  }
//}
case class StalledInfo(
                        time: Option[Time],
                        token: Option[Token],
                        data: Option[StalledData],
                      )

case class StalledData(buffering: Buffering)
object StalledData extends PlaybackJsonFields {
  implicit val data: Reads[StalledData] =
    (JsPath \ dataField).read[Buffering].map(StalledData.apply)
}

case class Buffering(currentResolution: CurrentResolution)
object Buffering extends PlaybackJsonFields {
  implicit val buffering: Reads[Buffering] =
    (JsPath \ bufferingField).read[CurrentResolution].map(Buffering.apply)
}


case class CurrentResolution(value: Option[Int])
object CurrentResolution extends PlaybackJsonFields {
  implicit val currentResolution: Reads[CurrentResolution] =
    (JsPath \ currentResolutionField).readNullable[Int].map(CurrentResolution.apply)
}
