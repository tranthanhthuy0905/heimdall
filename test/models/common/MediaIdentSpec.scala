package models.common

import org.scalatestplus.play._
import java.util.UUID.randomUUID

class MediaIdentSpec extends PlaySpec {

  "Media Identifier" must {
    "return a string for a single media identifier" in {
      val fileId = randomUUID
      val evidenceId = randomUUID
      val partnerId = randomUUID
      val result: String = MediaIdent(List(fileId), List(evidenceId), partnerId).toQueryString
      result mustBe s"file_id=$fileId&evidence_id=$evidenceId&partner_id=$partnerId"
    }

    "return a string for a set of two media idents" in {
      val fileIds = List(randomUUID, randomUUID)
      val evidenceIds = List(randomUUID, randomUUID)
      val partnerId = randomUUID
      val result: String = MediaIdent(fileIds, evidenceIds, partnerId).toQueryString
      val expected: String = s"file_id=${fileIds(0)},${fileIds(1)}&evidence_id=${evidenceIds(0)},${evidenceIds(1)}&partner_id=$partnerId"
      result mustBe expected
    }

  }

}
