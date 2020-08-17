package services.xpreport.playback

import play.api.libs.json.JsValue

trait PlaybackJsonConversions {

  def playbackInfoFromJson(json: JsValue): EventsInfo = {
   EventsInfo(
     json.asOpt[Time],
     json.asOpt[Token],
     json.asOpt[Data],
   )
  }

  def stalledInfoFromJson(json: JsValue): StalledInfo = {
    StalledInfo(
      json.asOpt[Time],
      json.asOpt[Token],
      json.asOpt[Event],
      json.asOpt[StalledData],
    )
  }

  // Return lagRatio of each resolution and aggregate for all resolution
  //   lagRatioAll = (sum totalViewDuration of all res + sum totalBufferingTime of all res) / (sum of totalViewDuration of all res)
  //   lagRatio of each res = (sum totalViewDuration of each res + sum totalBufferingTime of each res) / (sum of totalViewDuration of each res)
  // Return lagRatio of all resolution
  //   Eg. Seq(
  //       "lag_ratio_all" -> lagRatioAll,
  //       "input_delay_all" -> totalInputDelay
  //       "buffering_time_all" -> totalViewDuration
  //       "lag_ratio_all" -> lagRatioAll
  //       "lag_ratio_360" -> lagRatio360,
  //       "lag_ratio_480" -> lagRatio480
  //       ...
  //     )
  def logEventsInfoDetail(eventsInfo: EventsInfo): Seq[(String, Any)] = {
    var ret = Seq[(String, Any)]()

    for {
      eventTime <- eventsInfo.time
      inputToken <- eventsInfo.token
    } {
      val time = parseTime(eventTime)
      val token = parseToken(inputToken)
      ret = ret ++ Seq(
        "stream_token" -> token,
        "event_time" -> time,
      )
    }

    // Not log when there's no data in the body
    for {
      data <- eventsInfo.data
      browserName = data.browserName.flatMap(_.value).getOrElse("unknown")
      fileExtension = data.fileExtension.flatMap(_.value).getOrElse("unknown")
    } {
      ret = ret ++ Seq(
        "browser_name" -> browserName,
        "file_extension" -> fileExtension,
      )
    }

    ret ++ lagRatioAll(eventsInfo) ++ lagRatioByResolution(eventsInfo)
  }

  // Calculate Lag Ratio for all resolution at that time
  //   lagRatioAll = (sum totalViewDuration of all res + sum totalBufferingTime of all res) / (sum of totalViewDuration of all res)
  // Return lagRatio of all resolution
  //   Eg.
  //   Seq(
  //     "buffering_time_all" -> totalBufferingTime
  //     "input_delay_all" -> totalInputDelay
  //     "buffering_time_all" -> totalViewDuration
  //     "lag_ratio_all" -> lagRatioAll
  //   )
  private def lagRatioAll(eventsInfo: EventsInfo): Seq[(String, Any)] = {
    val data = eventsInfo.data.map(x => x.aggregationEvents.data)
    val durList = data.map(y => y.map(kv => kv._2))
    val totalBufferingTime = durList.map(_.foldLeft(0.0)(_ + _.totalBufferingTime.getOrElse(0.0))).getOrElse(0.0)
    val totalInputDelay    = durList.map(_.foldLeft(0.0)(_ + _.totalInputDelay.getOrElse(0.0))).getOrElse(0.0)
    val totalViewDuration  = durList.map(_.foldLeft(0.0)(_ + _.totalViewDuration.getOrElse(0.0))).getOrElse(0.0)
    val lagRatio = if (totalViewDuration != 0) (totalBufferingTime + totalInputDelay) / totalViewDuration else 0.0

    Seq(
      "buffering_time_all" -> totalBufferingTime,
      "input_delay_all" -> totalInputDelay,
      "view_duration_all" -> totalViewDuration,
      "lag_ratio_all" -> lagRatio
    )
  }

  // Calculate Lag Ratio for all resolution at that time
  //   lagRatio of each res = (sum totalViewDuration of each res + sum totalBufferingTime of each res) / (sum of totalViewDuration of each res)
  // Return a list of lagRatio of each resolution
  //   Eg. Seq(
  //   "lag_ratio_360" -> lagRatio360,
  //   "input_delay_360" -> totalInputDelay360
  //   "buffering_time_360" -> totalViewDuration360
  //   "view_duration_360" -> viewDuration360
  //   "lag_ratio_480" -> lagRatio480 ...
  //   )
  private def lagRatioByResolution(eventsInfo: EventsInfo): Seq[(String, Any)] = {
    var ret = Seq[(String, Any)]()

    for {
      map <- eventsInfo.data.map(x => x.aggregationEvents.data)
      (resolution, duration) <- map
    } {
      val bufferingTime = duration.totalBufferingTime.getOrElse(0.0)
      val inputDelay = duration.totalInputDelay.getOrElse(0.0)
      val viewDuration = duration.totalViewDuration.getOrElse(0.0)
      val lagRatio = if (viewDuration != 0) (bufferingTime + inputDelay) / viewDuration else 0.0
      ret = ret ++ Seq(
        "buffering_time_" + resolution -> bufferingTime,
        "input_delay_" + resolution -> inputDelay,
        "view_duration_" + resolution -> viewDuration,
        "lag_ratio_" + resolution -> lagRatio
      )
    }

    ret
  }

  private def parseTime(inputTime: Time): String = {
    inputTime match {
      case Time(Some(time)) => time
      case _ => "unknown"
    }
  }

  private def parseToken(inputToken: Token): String = {
    inputToken match {
      case Token(Some(token)) => token
      case _ => "unknown"
    }
  }

  def logStalledInfoDetail(stalledInfo: StalledInfo): Seq[(String, Any)] = {
    var ret = Seq[(String, Any)]()

    for {
      eventTime <- stalledInfo.time
      eventState <- stalledInfo.event
      stalledData <- stalledInfo.data
      inputToken <- stalledInfo.token
      buffering = stalledData.buffering
    } {
      val time = parseTime(eventTime)
      val token = parseToken(inputToken)
      val event = eventState.value.getOrElse("START")

      val browserName = stalledData.browserName.flatMap(_.value).getOrElse("unknown")
      val fileExtension = stalledData.fileExtension.flatMap(_.value).getOrElse("unknown")
      ret = ret ++ Seq(
        "browser_name" -> browserName,
        "file_extension" -> fileExtension,
      )

      val bufferingResolution = buffering.currentResolution.flatMap(_.value).getOrElse(-1)
      val bufferingDuration = buffering.stalledDuration.flatMap(_.value).getOrElse(-1)
      val bufferingReason = buffering.stalledReason.flatMap(_.value).getOrElse("unknown")

      ret = ret ++ Seq(
        "stream_token" -> token,
        "event_time" -> time,
        "event_state" -> event,
      )

      if (bufferingResolution != -1) {
        ret = ret ++ Seq(
          "buffering_resolution" -> bufferingResolution,
        )
      }

      if (bufferingDuration != -1) {
        ret = ret ++ Seq(
          "buffering_duration" -> bufferingDuration,
        )
      }

      if (bufferingReason != "unknown") {
        ret = ret ++ Seq(
          "buffering_reason" -> bufferingReason,
        )
      }
    }
    ret
  }
}
