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
//  event: String
//  data: {
//    buffering: {
//       currentResolution: string,
//       duration: Number
//    }
//    inputDelay: {
//       reason: String
//       duration: Number
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

case class StalledData(buffering: Buffering, inputDelay: InputDelay)
object StalledData extends PlaybackJsonFields {
  implicit val data: Reads[StalledData] = (
    (JsPath \ dataField).read[Buffering] and
      (JsPath \ dataField).read[InputDelay]
    )(StalledData.apply _)
}

case class Buffering(currentResolution: Option[CurrentResolution], stalledDuration: Option[StalledDuration])
object Buffering extends PlaybackJsonFields {
  implicit val buffering: Reads[Buffering] = (
    (JsPath \ bufferingField).readNullable[CurrentResolution] and
      (JsPath \ bufferingField).readNullable[StalledDuration]
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

case class InputDelayReason(value: Option[String])
object InputDelayReason extends PlaybackJsonFields {
  implicit val inputDelayReason: Reads[InputDelayReason] =
    (JsPath \ inputDelayReasonField).readNullable[String].map(InputDelayReason.apply)
}

case class InputDelay(inputDelayReason: Option[InputDelayReason], stalledDuration: Option[StalledDuration])
object InputDelay extends PlaybackJsonFields {
  implicit val inputDelay: Reads[InputDelay] = (
    (JsPath \ inputDelayField).readNullable[InputDelayReason] and
      (JsPath \ inputDelayField).readNullable[StalledDuration]
    )(InputDelay.apply _)
}
