package services.zookeeper

import com.evidence.service.common.zookeeper.ServiceEndpoint

case class PerftrakDatum(
  endpoint: ServiceEndpoint,
  planeComputational: Option[PlaneComputational],
  planeCaching: Option[PlaneCaching]) {

  lazy val planeComputationalAggregate: Double = {
    planeComputational.map(p => math.max(0.0, p.aggregate)).getOrElse(0.0)
  }

  lazy val isPlaneCachingEmpty: Boolean = {
    planeCaching.exists(_.tops.isEmpty)
  }
}
