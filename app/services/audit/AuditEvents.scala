package services.audit

import play.api.libs.json._
import com.evidence.service.audit.Tid

case class AuditEventParams(eventId: String, targetTid: Tid, updatedTid: Tid, json: String)

trait AuditEvent {

  /**
    * Audit event version.
    */
  final val ver = "1"

  /**
    * Evidence TID (target TID) consists of the 'evidence' entity type, evidence Id (id), and partner Id (domain).
    * For example:
    * evidence:48131afd2f4c44eb8a76b678b173f722@f3d719bcdb2b4b71bfb1436240fb9099
    */
  val targetTid: Tid

  /**
    * Authenticated TID consists of entity type received from jwtWrapper.subjectType (entity),
    * user_id (id), partner_id (domain), resource_id (resource).
    * For example:
    * subscriber:bda513fecfb94acbbb25a665387c12bd@f3d719bcdb2b4b71bfb1436240fb9099/www
    */
  val updatedByTid: Tid

  /**
    * File TID consists of the 'file' entity type, file Id (id), and partner Id (domain).
    * For example:
    * file:b48a7449f5ec4447817630383a354431@f3d719bcdb2b4b71bfb1436240fb9099
    * @note it does not look like fileId is used for buffered and streamed events.
    */
  val fileTid: Tid

  /**
    * The client IP address.
    * Retrieves the last untrusted proxy from the Forwarded-Headers or the X-Forwarded-*-Headers.
    */
  val remoteAddress: String

  /**
    * eventTypeUuid is a MD5 hash of the Ecomsaas class used to parse the event Event type.
    */
  val eventTypeUuid: String

  /**
    * toJsonString serializes data into JSON formatted message converted to a string.
    */
  def toJsonString: String = {
    Json
      .obj(
        "TargetTID" -> Json.obj(
          "Entity" -> targetTid.entity.value,
          "Domain" -> targetTid.domain,
          "ID"     -> targetTid.id
        ),
        "UpdatedByTID" -> Json.obj(
          "Entity" -> updatedByTid.entity.value,
          "Domain" -> updatedByTid.domain,
          "ID"     -> updatedByTid.id
        ),
        "FileTID" -> Json.obj(
          "Entity" -> fileTid.entity.value,
          "Domain" -> fileTid.domain,
          "ID"     -> fileTid.id
        ),
        "Ver"             -> ver,
        "ClientIpAddress" -> remoteAddress
      )
      .toString
  }

}

/**
  * EvidenceRecordBufferedEvent definition over AuditEvent abstraction.
  *
  * Defines eventTypeUuid as MD5 hash of the Ecomsaas class used to parse the event:
  *
  * echo -n com.evidence.data.evidence.events.v2.EvidenceRecordBuffered | md5
  *
  * @see https://git.taservs.net/ecom/ecomsaas/blob/master/wc/com.evidence/com.evidence/data/evidence/events/v2/EvidenceRecordBuffered.cs
  * @see https://axon.quip.com/Vfr8AP3bgX8u/Audit-Events-System-Overview
  * @see https://git.taservs.net/ecom/reporter/blob/fcd98eaa3baac4eec6619c74e7d395c2ee9be12a/reporter-service/src/main/scala/com/evidence/service/reporter/audit/events/EventUUIDToDescriptorMapper.scala#L120
  *
  * Example of EvidenceFileBuffered event retrieved from cassandra:
  *   select * from evidence_events_feed where
  *     partner_id=f3d719bc-db2b-4b71-bfb1-436240fb9099 and
  *     entity_id=31b4f97f-20cd-40de-acb8-7a6fe95357eb and
  *     event_type=513dd980-dafd-e1c5-0ab3-f9099657179e;
  *
  * {
  * "SessionID":"3305007100e84bc89a125aa69a8f89f4",
  * "ClientIpAddress":"10.141.32.62",
  * "RemoteIpAddr":"10.141.32.62",
  * "TargetTID":{
  * "Entity":5, // enum value for evidence
  * "Domain":"f3d719bcdb2b4b71bfb1436240fb9099",
  * "ID":"31b4f97f20cd40deacb87a6fe95357eb",
  * "Resource":""
  * },
  * "UpdatedByTID":{
  * "Entity":1,
  * "Domain":"f3d719bcdb2b4b71bfb1436240fb9099",
  * "ID":"bda513fecfb94acbbb25a665387c12bd",
  * "Resource":"www"
  * },
  * "Ver":"1",
  * "DateCreated":"2018-12-06T22:29:51.0314962Z",
  * "EventState":1
  * }
  *
  */
case class EvidenceRecordBufferedEvent(
  targetTid: Tid,
  updatedByTid: Tid,
  fileTid: Tid,
  remoteAddress: String
) extends AuditEvent {
  val eventTypeUuid = "513dd980-dafd-e1c5-0ab3-f9099657179e"
}

/**
  * EvidenceFileStreamedEvent definition over AuditEvent abstraction.
  *
  * Defines eventTypeUuid as MD5 hash of the Ecomsaas class used to parse the event:
  *
  * echo -n com.evidence.data.evidence.events.v2.EvidenceFileStreamed | md5
  *
  * @see https://git.taservs.net/ecom/ecomsaas/blob/master/wc/com.evidence/com.evidence/data/evidence/events/v2/EvidenceFileStreamed.cs
  * @see https://axon.quip.com/Vfr8AP3bgX8u/Audit-Events-System-Overview
  * @see https://git.taservs.net/ecom/reporter/blob/fcd98eaa3baac4eec6619c74e7d395c2ee9be12a/reporter-service/src/main/scala/com/evidence/service/reporter/audit/events/EventUUIDToDescriptorMapper.scala#L131
  *
  * Example of EvidenceFileStreamed event retrieved from cassandra:
  *   select * from evidence_events_feed where
  *     partner_id=f3d719bc-db2b-4b71-bfb1-436240fb9099 and
  *     entity_id=31b4f97f-20cd-40de-acb8-7a6fe95357eb and
  *     event_type=289e31fd-acbe-db5a-d6a6-b512ba23f76d;
  *
  * {
  * "FileTID":{
  * "Entity":6, // enum value for file
  * "Domain":"f3d719bcdb2b4b71bfb1436240fb9099",
  * "ID":"4f706842e6024287b80ba74b09d8995a",
  * "Resource":""
  * },
  * "SessionID":"2f60375d7c4a4d9aa6848b221debecb5",
  * "ClientIpAddress":"10.141.32.60",
  * "RemoteIpAddr":"10.141.32.60",
  * "TargetTID":{
  * "Entity":5, // enum value for evidence
  * "Domain":"f3d719bcdb2b4b71bfb1436240fb9099",
  * "ID":"31b4f97f20cd40deacb87a6fe95357eb",
  * "Resource":""
  * },
  * "UpdatedByTID":{
  * "Entity":1,
  * "Domain":"f3d719bcdb2b4b71bfb1436240fb9099",
  * "ID":"bda513fecfb94acbbb25a665387c12bd",
  * "Resource":"www"
  * },
  * "Ver":"1",
  * "DateCreated":"2018-10-22T18:35:02.6593351Z",
  * "EventState":1
  * }
  *
  */
case class EvidenceFileStreamedEvent(
  targetTid: Tid,
  updatedByTid: Tid,
  fileTid: Tid,
  remoteAddress: String
) extends AuditEvent {
  val eventTypeUuid = "289e31fd-acbe-db5a-d6a6-b512ba23f76d"
}

case class EvidenceViewed(
  targetTid: Tid,
  updatedByTid: Tid,
  fileTid: Tid,
  remoteAddress: String
) extends AuditEvent {
  final val eventTypeUuid = "321c3a6e-d748-3b1e-8222-c4a50e8e7d69"
}
