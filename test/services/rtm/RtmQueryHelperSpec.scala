package services.rtm

import org.scalatestplus.play.PlaySpec
import java.util.UUID.randomUUID

class RtmQueryValidatorSpec extends PlaySpec {
  val uuid      = randomUUID
  val extraUuid = randomUUID

  "Query Validator" must {

    "return validated query and ignore extra UUID-s" in {

      val query = Map(
        "file_id"     -> Seq(uuid.toString, extraUuid.toString, extraUuid.toString),
        "evidence_id" -> Seq(uuid.toString, extraUuid.toString, extraUuid.toString),
        "partner_id"  -> Seq(uuid.toString, extraUuid.toString, extraUuid.toString),
        "autoFixPTS"  -> Seq("true"),
        "segmentVer"  -> Seq("0")
      )

      val result = RtmQueryHelper("/media/hls/master", query)
      result mustBe Some(
        RtmQueryParams(
          path = "/hls/master",
          params = Map(
            "autoFixPTS" -> "true",
            "segmentVer" -> "0",
          )
        )
      )
    }

    "filter out extra parameter" in {

      val query = Map(
        "file_id"          -> Seq(uuid.toString),
        "evidence_id"      -> Seq(uuid.toString),
        "partner_id"       -> Seq(uuid.toString),
        "unexpected_param" -> Seq(randomUUID.toString, randomUUID.toString)
      )

      val result = RtmQueryHelper("/media/hls/master", query)
      result mustBe Some(RtmQueryParams(path = "/hls/master", params = Map()))
    }

    "return query and filter out unexpected_param despite missing file_id" in {

      val query = Map(
        "evidence_id"      -> Seq(uuid.toString),
        "partner_id"       -> Seq(uuid.toString),
        "unexpected_param" -> Seq(randomUUID.toString, randomUUID.toString)
      )

      val result = RtmQueryHelper("/media/hls/master", query)
      result mustBe Some(RtmQueryParams(path = "/hls/master", params = Map()))
    }

  }

}
