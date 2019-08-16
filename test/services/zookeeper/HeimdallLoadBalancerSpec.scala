package services.zookeeper

import java.util

import com.evidence.service.common.zookeeper.ServiceEndpoint
import org.apache.curator.framework.listen.ListenerContainer
import org.apache.curator.framework.recipes.cache.{ChildData, PathChildrenCacheListener}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.mockito.Mockito._

class HeimdallLoadBalancerSpec extends PlaySpec with MockitoSugar {

  import ZookeeperPackageTestHelper._

  private class HeimdallLoadBalancerMockContext(
    rtmChildrenCount: Int,
    perftrakChildrenCount: Int,
    minAgg: Int = 1,
    maxAgg: Int = 10) {
    val (rtmCacheMock, rtmListenerContainer)           = newCacheMock
    val (perftrakCacheMock, perftrakListenerContainer) = newCacheMock

    private def newCacheMock: (PathChildrenCacheFacade, ListenerContainer[PathChildrenCacheListener]) = {
      val cacheMock         = mock[PathChildrenCacheFacade]
      val listenerContainer = new ListenerContainer[PathChildrenCacheListener]
      when(cacheMock.getListenable) thenReturn listenerContainer
      (cacheMock, listenerContainer)
    }

    val lastKey: String                     = someKey
    val listOfRtmData: util.List[ChildData] = newListOfRtmChildData(rtmChildrenCount)

    val listOfPerftrakData: util.List[ChildData] =
      newListOfPerftrakChildData(perftrakChildrenCount, minAgg, maxAgg, 128, lastKey)

    when(rtmCacheMock.getCurrentData) thenReturn listOfRtmData
    when(perftrakCacheMock.getCurrentData) thenReturn listOfPerftrakData
  }

  "HeimdallLoadBalancer" should {

    "return None when there is no servers available" in new HeimdallLoadBalancerMockContext(0, 0) {
      val loadBalancer: HeimdallLoadBalancer =
        new HeimdallLoadBalancer(
          rtmCacheMock,
          perftrakCacheMock,
          HeimdallLoadBalancerConfig(enableCache = true, reloadIntervalMs = 100))
      loadBalancer.start()
      val result: Option[ServiceEndpoint] = loadBalancer.getInstance(someKey)
      result mustBe None
    }

    "return the single existing endpoint" in new HeimdallLoadBalancerMockContext(1, 1) {
      val loadBalancer: HeimdallLoadBalancer =
        new HeimdallLoadBalancer(
          rtmCacheMock,
          perftrakCacheMock,
          HeimdallLoadBalancerConfig(enableCache = true, reloadIntervalMs = 100))
      loadBalancer.start()
      val result: Option[ServiceEndpoint]   = loadBalancer.getInstance(someKey)
      val expected: Option[ServiceEndpoint] = Some(ServiceEndpoint(newHostName(), 8900))
      result mustBe expected
    }

    "get endpoint from the topContributingKeysMap" in new HeimdallLoadBalancerMockContext(60, 60) {
      val loadBalancer: HeimdallLoadBalancer =
        new HeimdallLoadBalancer(
          rtmCacheMock,
          perftrakCacheMock,
          HeimdallLoadBalancerConfig(enableCache = true, reloadIntervalMs = 100))
      loadBalancer.start()
      val result: Option[ServiceEndpoint]   = loadBalancer.getInstance(lastKey)
      val expected: Option[ServiceEndpoint] = Some(ServiceEndpoint(newHostName(60), 8900))
      result mustBe expected
    }

    "return 002 endpoint from the cache" in new HeimdallLoadBalancerMockContext(2, 2, 0, 0) {
      val loadBalancer: HeimdallLoadBalancer =
        new HeimdallLoadBalancer(
          rtmCacheMock,
          perftrakCacheMock,
          HeimdallLoadBalancerConfig(enableCache = true, reloadIntervalMs = 100))
      loadBalancer.start()
      val result: Option[ServiceEndpoint]   = loadBalancer.getInstance(someKey)
      val expected: Option[ServiceEndpoint] = Some(ServiceEndpoint(newHostName(2), 8900))
      result mustBe expected
    }

  }

}
