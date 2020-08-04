package services.xpreport.playback

import org.scalatestplus.play.PlaySpec
import play.api.libs.json.Json
import utils.JsonFormat

class PlaybackJsonConversionsSpec extends PlaySpec with PlaybackJsonConversions with PlaybackJsonFields with JsonFormat {

  val res360  = "360"
  val res480  = "480"
  val res720  = "720"
  val res1080 = "1080"
  val res1440 = "1440"
  val res2160 = "2160"

  val streamToken = "abcde12345"
  val time = "2018:10:19 16:56:15"

  "PlaybackJsonConversionsSpec" should {
    "parse playback info and calculate lagRatio correctly with full playback json" in {
      val resolution = Seq(
        res360,
        res480,
        res720,
        res1080,
        res1440,
        res2160,
      )

      val viewDuration = Map(
        res360 -> 111.0,
        res480 -> 444.0,
        res720 -> 123.0,
        res1080 -> 223.1,
        res1440 -> 357.1,
        res2160 -> 123.4,
      )

      val inputDelay = Map(
        res360 -> 222.0,
        res480 -> 555.0,
        res720 -> 234.0,
        res1080 -> 331.2,
        res1440 -> 246.2,
        res2160 -> 567.8,
      )

      val bufferingTime = Map(
        res360 -> 0.0,
        res480 -> 666.0,
        res720 -> 552.5,
        res1080 -> 112.3,
        res1440 -> 890.0,
        res2160 -> 901.2,
      )
      val playbackInfoJson =
        s"""
           |{
           |    "$eventTimeField" : "$time",
           |    "$tokenField" : "$streamToken",
           |    "$dataField" : {
           |        "$aggregationEventsField": {
           |            "$res360" : {
           |                "$totalViewDurationField": ${viewDuration(res360)},
           |                "$totalInputDelayField": ${inputDelay(res360)},
           |                "$totalBufferingTimeField": ${bufferingTime(res360)}
           |            },
           |            "$res480" : {
           |                "$totalViewDurationField": ${viewDuration(res480)},
           |                "$totalInputDelayField": ${inputDelay(res480)},
           |                "$totalBufferingTimeField": ${bufferingTime(res480)}
           |            },
           |            "$res720" : {
           |                "$totalViewDurationField": ${viewDuration(res720)},
           |                "$totalInputDelayField": ${inputDelay(res720)},
           |                "$totalBufferingTimeField": ${bufferingTime(res720)}
           |            },
           |            "$res1080" : {
           |                "$totalViewDurationField": ${viewDuration(res1080)},
           |                "$totalInputDelayField": ${inputDelay(res1080)},
           |                "$totalBufferingTimeField": ${bufferingTime(res1080)}
           |            },
           |            "$res1440" : {
           |                "$totalViewDurationField": ${viewDuration(res1440)},
           |                "$totalInputDelayField": ${inputDelay(res1440)},
           |                "$totalBufferingTimeField": ${bufferingTime(res1440)}
           |            },
           |            "$res2160" : {
           |                "$totalViewDurationField": ${viewDuration(res2160)},
           |                "$totalInputDelayField": ${inputDelay(res2160)},
           |                "$totalBufferingTimeField": ${bufferingTime(res2160)}
           |            }
           |        }
           |    }
           |}
           |""".stripMargin

      val playbackInfo = playbackInfoFromJson(Json.parse(playbackInfoJson))
      val expectPlaybackInfo = EventsInfo(
        time = Some(Time(Some(time))),
        token = Some(Token(Some(streamToken))),
        data = Some(
          Data(
            AggregationEvents(
              Map(
                res360 -> Duration(
                  totalViewDuration = Some(viewDuration(res360)),
                  totalInputDelay = Some(inputDelay(res360)),
                  totalBufferingTime = Some(bufferingTime(res360))
                ),
                res480 -> Duration(
                  totalViewDuration = Some(viewDuration(res480)),
                  totalInputDelay = Some(inputDelay(res480)),
                  totalBufferingTime = Some(bufferingTime(res480))
                ),
                res720 -> Duration(
                  totalViewDuration = Some(viewDuration(res720)),
                  totalInputDelay = Some(inputDelay(res720)),
                  totalBufferingTime = Some(bufferingTime(res720))
                ),
                res1080 -> Duration(
                  totalViewDuration = Some(viewDuration(res1080)),
                  totalInputDelay = Some(inputDelay(res1080)),
                  totalBufferingTime = Some(bufferingTime(res1080))
                ),
                res1440 -> Duration(
                  totalViewDuration = Some(viewDuration(res1440)),
                  totalInputDelay = Some(inputDelay(res1440)),
                  totalBufferingTime = Some(bufferingTime(res1440))
                ),
                res2160 -> Duration(
                  totalViewDuration = Some(viewDuration(res2160)),
                  totalInputDelay = Some(inputDelay(res2160)),
                  totalBufferingTime = Some(bufferingTime(res2160))
                ),
              )
            )
          )
        ),
      )
      playbackInfo mustBe expectPlaybackInfo

      val lagRatio = logEventsInfoDetail(eventsInfo = playbackInfo)

      val expectLagRatio = Seq(
        "stream_token" -> streamToken,
        "event_time" -> time,
        "lag_ratio_all" -> (inputDelay.foldLeft(0.0)(_ + _._2) + bufferingTime.foldLeft(0.0)(_ + _._2)) / viewDuration.foldLeft(0.0)(_ + _._2),
        "input_delay_all" -> inputDelay.foldLeft(0.0)(_ + _._2),
        "view_duration_all" -> viewDuration.foldLeft(0.0)(_ + _._2),
        "buffering_time_all" -> bufferingTime.foldLeft(0.0)(_ + _._2),
      ) ++ resolution.flatMap( x =>
        Seq(
          "buffering_time_" + x -> bufferingTime(x),
          "input_delay_" + x -> inputDelay(x),
          "view_duration_" + x -> viewDuration(x),
          "lag_ratio_" + x -> (inputDelay(x) + bufferingTime(x)) / viewDuration(x)
        )
      )

      lagRatio.toSet mustBe expectLagRatio.toSet
    }

    "parse playback info and calculate lagRatio correctly if missing some parts of resolution" in {
      val resolution = Seq(
        res360,
        res480,
      )

      val viewDuration = Map(
        res360 -> 111.0,
        res480 -> 444.0,
      )

      val inputDelay = Map(
        res360 -> 222.0,
        res480 -> 555.0,
      )

      val bufferingTime = Map(
        res360 -> 0.0,
        res480 -> 666.0,
      )
      val playbackInfoJson =
        s"""
           |{
           |    "$tokenField" : "$streamToken",
           |    "$dataField" : {
           |        "$aggregationEventsField": {
           |            "$res360" : {
           |                "$totalViewDurationField": ${viewDuration(res360)},
           |                "$totalInputDelayField": ${inputDelay(res360)},
           |                "$totalBufferingTimeField": ${bufferingTime(res360)}
           |            },
           |            "$res480" : {
           |                "$totalViewDurationField": ${viewDuration(res480)},
           |                "$totalInputDelayField": ${inputDelay(res480)},
           |                "$totalBufferingTimeField": ${bufferingTime(res480)}
           |            }
           |        }
           |    }
           |}
           |""".stripMargin

      val playbackInfo = playbackInfoFromJson(Json.parse(playbackInfoJson))
      val expectPlaybackInfo = EventsInfo(
        time = Some(Time(None)),
        token = Some(Token(Some(streamToken))),
        data = Some(
          Data(
            AggregationEvents(
              Map(
                res360 -> Duration(
                  totalViewDuration = Some(viewDuration(res360)),
                  totalInputDelay = Some(inputDelay(res360)),
                  totalBufferingTime = Some(bufferingTime(res360)),
                ),
                res480 -> Duration(
                  totalViewDuration = Some(viewDuration(res480)),
                  totalInputDelay = Some(inputDelay(res480)),
                  totalBufferingTime = Some(bufferingTime(res480)),
                )
              )
            )
          )
        ),
      )
      playbackInfo mustBe expectPlaybackInfo

      val lagRatio = logEventsInfoDetail(eventsInfo = playbackInfo)
      val expectLagRatio = Seq(
        "stream_token" -> streamToken,
        "event_time" -> "unknown",
        "lag_ratio_all" -> (inputDelay.foldLeft(0.0)(_ + _._2) + bufferingTime.foldLeft(0.0)(_ + _._2)) / viewDuration.foldLeft(0.0)(_ + _._2),
        "input_delay_all" -> inputDelay.foldLeft(0.0)(_ + _._2),
        "view_duration_all" -> viewDuration.foldLeft(0.0)(_ + _._2),
        "buffering_time_all" -> bufferingTime.foldLeft(0.0)(_ + _._2),
      ) ++ resolution.flatMap( x =>
        Seq(
          "buffering_time_" + x -> bufferingTime(x),
          "input_delay_" + x -> inputDelay(x),
          "view_duration_" + x -> viewDuration(x),
          "lag_ratio_" + x -> (inputDelay(x) + bufferingTime(x)) / viewDuration(x)
        )
      )
      lagRatio.toSet mustBe expectLagRatio.toSet
    }

    "parse playback info and calculate lagRatio correctly if missing data and time" in {
      val playbackInfoJson =
        s"""
           |{
           |    "$tokenField" : "$streamToken"
           |}
           |""".stripMargin

      val playbackInfo = playbackInfoFromJson(Json.parse(playbackInfoJson))
      val expectPlaybackInfo = EventsInfo(
        time = Some(Time(None)),
        token = Some(Token(Some(streamToken))),
        data = None
      )
      playbackInfo mustBe expectPlaybackInfo

      val lagRatio = logEventsInfoDetail(eventsInfo = playbackInfo)
      val expectLagRatio = Seq(
        "stream_token" -> streamToken,
        "event_time" -> "unknown",
        "view_duration_all" -> 0.0,
        "buffering_time_all" -> 0.0,
        "input_delay_all" -> 0.0,
        "lag_ratio_all" -> 0.0,
      )
      lagRatio.toSet mustBe expectLagRatio.toSet
    }

    "parse playback info and calculate lagRatio correctly if missing token" in {
      val resolution = Seq(
        res360,
        res480,
      )

      val viewDuration = Map(
        res360 -> 111.0,
        res480 -> 444.0,
      )

      val inputDelay = Map(
        res360 -> 222.0,
        res480 -> 555.0,
      )

      val bufferingTime = Map(
        res360 -> 0.0,
        res480 -> 666.0,
      )
      val playbackInfoJson =
        s"""
           |{
           |    "$dataField" : {
           |        "$aggregationEventsField": {
           |            "$res360" : {
           |                "$totalViewDurationField": ${viewDuration(res360)},
           |                "$totalInputDelayField": ${inputDelay(res360)},
           |                "$totalBufferingTimeField": ${bufferingTime(res360)}
           |            },
           |            "$res480" : {
           |                "$totalViewDurationField": ${viewDuration(res480)},
           |                "$totalInputDelayField": ${inputDelay(res480)},
           |                "$totalBufferingTimeField": ${bufferingTime(res480)}
           |            }
           |        }
           |    }
           |}
           |""".stripMargin

      val playbackInfo = playbackInfoFromJson(Json.parse(playbackInfoJson))
      val expectPlaybackInfo = EventsInfo(
        time = Some(Time(None)),
        token = Some(Token(None)),
        data = Some(
          Data(
            AggregationEvents(
              Map(
                res360 -> Duration(
                  totalViewDuration = Some(viewDuration(res360)),
                  totalInputDelay = Some(inputDelay(res360)),
                  totalBufferingTime = Some(bufferingTime(res360)),
                ),
                res480 -> Duration(
                  totalViewDuration = Some(viewDuration(res480)),
                  totalInputDelay = Some(inputDelay(res480)),
                  totalBufferingTime = Some(bufferingTime(res480)),
                )
              )
            )
          )
        ),
      )
      playbackInfo mustBe expectPlaybackInfo

      val lagRatio = logEventsInfoDetail(eventsInfo = playbackInfo)
      val expectLagRatio = Seq(
        "stream_token" -> "unknown",
        "event_time" -> "unknown",
        "lag_ratio_all" -> (inputDelay.foldLeft(0.0)(_ + _._2) + bufferingTime.foldLeft(0.0)(_ + _._2)) / viewDuration.foldLeft(0.0)(_ + _._2),
        "input_delay_all" -> inputDelay.foldLeft(0.0)(_ + _._2),
        "view_duration_all" -> viewDuration.foldLeft(0.0)(_ + _._2),
        "buffering_time_all" -> bufferingTime.foldLeft(0.0)(_ + _._2),
      ) ++ resolution.flatMap( x =>
        Seq(
          "buffering_time_" + x -> bufferingTime(x),
          "input_delay_" + x -> inputDelay(x),
          "view_duration_" + x -> viewDuration(x),
          "lag_ratio_" + x -> (inputDelay(x) + bufferingTime(x)) / viewDuration(x)
        )
      )
      lagRatio.toSet mustBe expectLagRatio.toSet
    }

    "parse playback info and return lagRatio = 0 if viewDuration is 0" in {
      val resolution = Seq(
        res360,
        res480,
      )

      val viewDuration = Map(
        res360 -> 0.0,
        res480 -> 0.0,
      )

      val inputDelay = Map(
        res360 -> 222.0,
        res480 -> 555.0,
      )

      val bufferingTime = Map(
        res360 -> 0.0,
        res480 -> 666.0,
      )
      val playbackInfoJson =
        s"""
           |{
           |    "$tokenField" : "$streamToken",
           |    "$dataField" : {
           |        "$aggregationEventsField": {
           |            "$res360" : {
           |                "$totalViewDurationField": ${viewDuration(res360)},
           |                "$totalInputDelayField": ${inputDelay(res360)},
           |                "$totalBufferingTimeField": ${bufferingTime(res360)}
           |            },
           |            "$res480" : {
           |                "$totalViewDurationField": ${viewDuration(res480)},
           |                "$totalInputDelayField": ${inputDelay(res480)},
           |                "$totalBufferingTimeField": ${bufferingTime(res480)}
           |            }
           |        }
           |    }
           |}
           |""".stripMargin

      val playbackInfo = playbackInfoFromJson(Json.parse(playbackInfoJson))
      val expectPlaybackInfo = EventsInfo(
        time = Some(Time(None)),
        token = Some(Token(Some(streamToken))),
        data = Some(
          Data(
            AggregationEvents(
              Map(
                res360 -> Duration(
                  totalViewDuration = Some(viewDuration(res360)),
                  totalInputDelay = Some(inputDelay(res360)),
                  totalBufferingTime = Some(bufferingTime(res360)),
                ),
                res480 -> Duration(
                  totalViewDuration = Some(viewDuration(res480)),
                  totalInputDelay = Some(inputDelay(res480)),
                  totalBufferingTime = Some(bufferingTime(res480)),
                )
              )
            )
          )
        ),
      )
      playbackInfo mustBe expectPlaybackInfo

      val lagRatio = logEventsInfoDetail(eventsInfo = playbackInfo)
      val expectLagRatio = Seq(
        "stream_token" -> streamToken,
        "event_time" -> "unknown",
        "lag_ratio_all" -> 0.0,
        "input_delay_all" -> inputDelay.foldLeft(0.0)(_ + _._2),
        "view_duration_all" -> viewDuration.foldLeft(0.0)(_ + _._2),
        "buffering_time_all" -> bufferingTime.foldLeft(0.0)(_ + _._2),
      ) ++ resolution.flatMap( x =>
        Seq(
          "buffering_time_" + x -> bufferingTime(x),
          "input_delay_" + x -> inputDelay(x),
          "view_duration_" + x -> viewDuration(x),
          "lag_ratio_" + x -> 0.0
        )
      )
      lagRatio.toSet mustBe expectLagRatio.toSet
    }

    "parse stalled info correctly if missing duration and reason" in {
      val event = "Start"
      val stalledInfoJson =
        s"""
           |{
           |    "$eventTimeField" : "$time",
           |    "$tokenField" : "$streamToken",
           |    "$eventField" : "$event",
           |    "$dataField" : {
           |        "$bufferingField": {
           |            "$currentResolutionField" : $res1080
           |        }
           |    }
           |}
           |""".stripMargin
      val stalledInfo = stalledInfoFromJson(Json.parse(stalledInfoJson))
      val expectStalledInfo = StalledInfo(
        time = Some(Time(Some(time))),
        token = Some(Token(Some(streamToken))),
        event = Some(Event(Some(event))),
        data = Some(
          StalledData(
            Buffering(
              Some(CurrentResolution(Some(res1080.toInt))),
              Some(StalledDuration(None)),
              Some(StalledReason(None))
            )
          )
        )
      )
      stalledInfo mustBe expectStalledInfo

      val logDetail = logStalledInfoDetail(stalledInfo)
      val expectedLogDetail = Seq(
        "stream_token" -> streamToken,
        "event_time" -> time,
        "event_state" -> event,
        "buffering_resolution" -> res1080.toInt,
      )
      logDetail mustBe expectedLogDetail
    }

    "parse stalled info correctly if missing token" in {
      val stalledInfoJson =
        s"""
           |{
           |    "$dataField" : {
           |        "$bufferingField": {
           |            "$currentResolutionField" : $res1080
           |        }
           |    }
           |}
           |""".stripMargin
      val stalledInfo = stalledInfoFromJson(Json.parse(stalledInfoJson))
      val expectStalledInfo = StalledInfo(
        time = Some(Time(None)),
        token = Some(Token(None)),
        event = Some(Event(None)),
        data = Some(
          StalledData(
            Buffering(
              Some(CurrentResolution(Some(res1080.toInt))),
              Some(StalledDuration(None)),
              Some(StalledReason(None))
            )
          )
        )
      )
      stalledInfo mustBe expectStalledInfo

      val logDetail = logStalledInfoDetail(stalledInfo)
      val expectedLogDetail = Seq(
        "stream_token" -> "unknown",
        "event_time" -> "unknown",
        "event_state" -> "START",
        "buffering_resolution" -> res1080.toInt
      )
      logDetail mustBe expectedLogDetail
    }

    "parse stalled info correctly if missing reason" in {
      val event = "End"
      val stalledDuration = 123.321
      val stalledInfoJson =
        s"""
           |{
           |    "$eventTimeField" : "$time",
           |    "$tokenField" : "$streamToken",
           |    "$eventField" : "$event",
           |    "$dataField" : {
           |        "$bufferingField": {
           |            "$currentResolutionField" : $res1080,
           |            "$stalledDurationField" : $stalledDuration
           |        }
           |    }
           |}
           |""".stripMargin
      val stalledInfo = stalledInfoFromJson(Json.parse(stalledInfoJson))
      val expectStalledInfo = StalledInfo(
        time = Some(Time(Some(time))),
        token = Some(Token(Some(streamToken))),
        event = Some(Event(Some(event))),
        data = Some(
          StalledData(
            Buffering(
              Some(CurrentResolution(Some(res1080.toInt))),
              Some(StalledDuration(Some(stalledDuration))),
              Some(StalledReason(None))
            )
          )
        )
      )
      stalledInfo mustBe expectStalledInfo

      val logDetail = logStalledInfoDetail(stalledInfo)
      val expectedLogDetail = Seq(
        "stream_token" -> streamToken,
        "event_time" -> time,
        "event_state" -> event,
        "buffering_resolution" -> res1080.toInt,
        "buffering_duration" -> stalledDuration,
      )
      logDetail mustBe expectedLogDetail
    }


    "parse stalled info correctly when have all info" in {
      val event = "End"
      val stalledDuration = 123.321
      val stalledReason = "Seek"
      val stalledInfoJson =
        s"""
           |{
           |    "$eventTimeField" : "$time",
           |    "$tokenField" : "$streamToken",
           |    "$eventField" : "$event",
           |    "$dataField" : {
           |        "$bufferingField": {
           |            "$currentResolutionField" : $res1080,
           |            "$stalledDurationField" : $stalledDuration,
           |            "$stalledReasonField" : "$stalledReason"
           |        }
           |    }
           |}
           |""".stripMargin
      val stalledInfo = stalledInfoFromJson(Json.parse(stalledInfoJson))
      val expectStalledInfo = StalledInfo(
        time = Some(Time(Some(time))),
        token = Some(Token(Some(streamToken))),
        event = Some(Event(Some(event))),
        data = Some(
          StalledData(
            Buffering(
              Some(CurrentResolution(Some(res1080.toInt))),
              Some(StalledDuration(Some(stalledDuration))),
              Some(StalledReason(Some(stalledReason)))
            )
          )
        )
      )
      stalledInfo mustBe expectStalledInfo

      val logDetail = logStalledInfoDetail(stalledInfo)
      val expectedLogDetail = Seq(
        "stream_token" -> streamToken,
        "event_time" -> time,
        "event_state" -> event,
        "buffering_resolution" -> res1080.toInt,
        "buffering_duration" -> stalledDuration,
        "buffering_reason" -> stalledReason,
      )
      logDetail mustBe expectedLogDetail
    }
  }
}
