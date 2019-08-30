package services.zookeeper

import org.apache.curator.framework.listen.ListenerContainer
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.mockito.Mockito._

class HeimdallLoadBalancerUK2Spec extends PlaySpec with MockitoSugar {

  import ZookeeperPackageTestHelper._

  private class HeimdallLoadBalancerMockUK2 {

    /**
      * The assumption is that consistent hash ring should distribute new requests corresponding to replica counts.
      * Old cached files will go to the endpoints as the map directs them too,
      * this will update the priority value and readjust the hash ring.
      */
    final val enableCache = false

    val (rtmCacheMock, rtmListenerContainer)           = newCacheMock
    val (perftrakCacheMock, perftrakListenerContainer) = newCacheMock

    private def newCacheMock: (PathChildrenCacheFacade, ListenerContainer[PathChildrenCacheListener]) = {
      val cacheMock         = mock[PathChildrenCacheFacade]
      val listenerContainer = new ListenerContainer[PathChildrenCacheListener]
      when(cacheMock.getListenable) thenReturn listenerContainer
      (cacheMock, listenerContainer)
    }

    val (rtmData, perftrakData) = getChildDataFromSplunkExport("assets/uk2_perftrak_20190823_122033_122045.json")

    when(rtmCacheMock.getCurrentData) thenReturn rtmData
    when(perftrakCacheMock.getCurrentData) thenReturn perftrakData
  }

  "HeimdallLoadBalancerUK2Spec" should {

    "create load balancer with data exported from UK2, and engage all nodes" in new HeimdallLoadBalancerMockUK2 {
      val loadBalancer: HeimdallLoadBalancer =
        new HeimdallLoadBalancer(
          rtmCacheMock,
          perftrakCacheMock,
          HeimdallLoadBalancerConfig(enableCache = enableCache, reloadIntervalMs = 100))

      loadBalancer.start()

      val keys = randomKeys(1000000)
      val endpoints     = keys.map(k => loadBalancer.getInstance(k))
      val replicaCounts = loadBalancer.getReplicaCounts
      val replicaSum = replicaCounts.foldLeft(0.0) {
        case (sum, e) => (sum + e._2)
      }

      val returnedEndpointCounts = endpoints.groupBy(e => e).map(e => (e._1.get, e._2.length))
      val returnedEndpointSum = returnedEndpointCounts.foldLeft(0.0) {
        case (sum, e) => (sum + e._2)
      }

      val deviations = replicaCounts map { replicaCount =>
        val endpoint = replicaCount._1
        returnedEndpointCounts.get(endpoint) match {
          case Some(found) =>
            val expected  = replicaCount._2.toFloat / replicaSum.toFloat
            val received  = found.toFloat / returnedEndpointSum.toFloat
            val deviation = 100.0 * (expected - received).abs / expected
            (endpoint -> deviation)
          case _ =>
            println(s"endpoint $endpoint didn't get assigned to any key")
            assert(false)
        }
      }
      println(s"calculated replicaCounts:\n $replicaCounts\nkeys distribution:\n $returnedEndpointCounts\ndeviations:\n $deviations")
      assert(deviations.size == replicaCounts.size)
    }

  }
}
