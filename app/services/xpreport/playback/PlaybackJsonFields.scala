package services.xpreport.playback

trait PlaybackJsonFields {
  // common
  protected final val tokenField              = "token"
  protected final val eventTimeField          = "time"
  protected final val dataField               = "data"
  protected final val browserNameField        = "browserName"
  protected final val fileExtensionField      = "fileExtension"

  // Aggregation events
  protected final val aggregationEventsField  = "aggregation"
  protected final val totalViewDurationField  = "totalViewDuration"
  protected final val totalInputDelayField    = "totalInputDelay"
  protected final val totalBufferingTimeField = "totalBufferingTime"

  // Stalled events
  protected final val bufferingField          = "buffering"
  protected final val currentResolutionField  = "currentResolution"
  protected final val stalledDurationField    = "duration"
  protected final val stalledReasonField      = "reason"
  protected final val eventField              = "event"
}

