package services.zookeeper

import com.evidence.service.common.zookeeper.ServiceEndpoint

case class PerftrakDatum(
  endpoint: ServiceEndpoint,
  planeComputational: Option[PlaneComputational],
  planeCaching: Option[PlaneCaching]) {

  def isPlaneCachingEmpty: Boolean = {
    planeCaching match {
      case Some(value) => value.tops.isEmpty
      case _           => true
    }
  }

}
