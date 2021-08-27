package services.sage

import com.axon.sage.protos.query.evidence_message.{Evidence => SageEvidenceProto}
import com.axon.sage.protos.common.path.ReadRequest_Path
import java.time.{Duration, Instant}
import java.util.UUID

case class QueryRequest(path: Seq[ReadRequest_Path])

case class EvidenceId(entityId: UUID, partnerId: UUID)

case class Evidence(evidenceId: UUID, partnerId: UUID, contentType: String)

object Evidence {
    def fromSageProto(evidence: SageEvidenceProto) : Evidence = {
        new Evidence(
            UUID.fromString(evidence.id),
            UUID.fromString(evidence.partnerId),
            evidence.contentType.getOrElse("")
        )
    }
}
