package utils

import java.time.{ZoneId, ZonedDateTime}
import java.time.format.DateTimeFormatter

object DateTime {

  def getUtcDate: String = {
    val utcZoneId: ZoneId = ZoneId.of("UTC")
    val utcNow = ZonedDateTime.now(utcZoneId)
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
    formatter.format(utcNow)
  }

}
