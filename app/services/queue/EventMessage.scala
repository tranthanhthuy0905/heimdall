package services.queue

trait EventMessage

case class VideoProbeEvent(partnerId: String, evidenceId: String, fileId: String) extends EventMessage
