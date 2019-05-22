package models.common

import org.scalatestplus.play.PlaySpec
import java.util.UUID.randomUUID

class QueryValidatorSpec extends PlaySpec {
  val uuid = randomUUID
  val extraUuid = randomUUID

  "Query Validator" must {

    "return validated query and ignore extra UUID-s" in {

      val query = Map(
        "file_id" -> Seq(uuid.toString, extraUuid.toString, extraUuid.toString),
        "evidence_id" -> Seq(uuid.toString, extraUuid.toString, extraUuid.toString),
        "partner_id" -> Seq(uuid.toString, extraUuid.toString, extraUuid.toString),
        "autoFixPTS" -> Seq("true")
      )

      val result = QueryHelper("/media/hls/master", query)
      result mustBe Some(
        RtmQueryParams(
          media = MediaIdent(List(uuid), List(uuid), uuid),
          path = "/hls/master",
          params = Map("autoFixPTS" -> "true")
        )
      )
    }

    "filter out extra parameter" in {

      val query = Map(
        "file_id" -> Seq(uuid.toString),
        "evidence_id" -> Seq(uuid.toString),
        "partner_id" -> Seq(uuid.toString),
        "unexpected_param" -> Seq(randomUUID.toString, randomUUID.toString)
      )

      val result = QueryHelper("/media/hls/master", query)
      result mustBe Some(RtmQueryParams(media = MediaIdent(List(uuid), List(uuid), uuid), path = "/hls/master", params = Map()))
    }

    "return None because of missing file_id" in {

      val query = Map(
        "evidence_id" -> Seq(uuid.toString),
        "partner_id" -> Seq(uuid.toString),
        "unexpected_param" -> Seq(randomUUID.toString, randomUUID.toString)
      )

      val result = QueryHelper("/media/hls/master", query)
      result mustBe None
    }

  }

}
