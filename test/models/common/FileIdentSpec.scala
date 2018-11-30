package models.common

import org.scalatestplus.play._
import java.util.UUID.randomUUID

class FileIdentSpec extends PlaySpec {

  "File Identifier" must {
    "return a string" in {
      val fileId = randomUUID
      val evidenceId = randomUUID
      val partnerId = randomUUID
      val file: String = FileIdent(fileId, evidenceId, partnerId).toString
      file mustBe s"file_id=$fileId&evidence_id=$evidenceId&partner_id=$partnerId"
    }
  }

}
