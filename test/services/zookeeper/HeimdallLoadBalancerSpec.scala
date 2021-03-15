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
    enableRTMv2: Boolean = false,
    minAgg: Int = 1,
    maxAgg: Int = 10) {

    var nodeAndPerftrackAware: Map[Int, (PathChildrenCacheFacade, PathChildrenCacheFacade)] = Map()
    var lastKey: Map[Int, String]                                                           = Map()

    private def newCacheMock: PathChildrenCacheFacade = {
      val cacheMock         = mock[PathChildrenCacheFacade]
      val listenerContainer = new ListenerContainer[PathChildrenCacheListener]
      when(cacheMock.getListenable) thenReturn listenerContainer
      cacheMock
    }

    val numberOfRTMVersion = if (enableRTMv2) 2 else 1
    for (apiVersion <- 1 to numberOfRTMVersion) {
      val rtmCacheMock      = newCacheMock
      val perftrakCacheMock = newCacheMock

      val lk: String                          = someKey
      val listOfRtmData: util.List[ChildData] = newListOfRtmChildData(rtmChildrenCount)
      val listOfPerftrakData: util.List[ChildData] =
        newListOfPerftrakChildData(perftrakChildrenCount, minAgg, maxAgg, lk)

      when(rtmCacheMock.getCurrentData) thenReturn listOfRtmData
      when(perftrakCacheMock.getCurrentData) thenReturn listOfPerftrakData

      nodeAndPerftrackAware = nodeAndPerftrackAware + (apiVersion -> (rtmCacheMock, perftrakCacheMock))
      lastKey = lastKey + (apiVersion                             -> lk)
    }
  }

  "HeimdallLoadBalancer" should {

    "return None when there is no servers available" in new HeimdallLoadBalancerMockContext(0, 0) {
      val loadBalancer: HeimdallLoadBalancer =
        new HeimdallLoadBalancer(
          nodeAndPerftrackAware,
          HeimdallLoadBalancerConfig(enableCache = true, reloadIntervalMs = 100))
      loadBalancer.start()
      val result: Option[ServiceEndpoint] = loadBalancer.getInstance(someKey, 1)
      result mustBe None
    }

    "return the single existing endpoint on each api version" in new HeimdallLoadBalancerMockContext(1, 1, true) {
      val loadBalancer: HeimdallLoadBalancer =
        new HeimdallLoadBalancer(
          nodeAndPerftrackAware,
          HeimdallLoadBalancerConfig(enableCache = true, reloadIntervalMs = 100))
      loadBalancer.start()
      nodeAndPerftrackAware.keySet.foreach(rtmVersion => {
        val result: Option[ServiceEndpoint]   = loadBalancer.getInstance(someKey, rtmVersion)
        val expected: Option[ServiceEndpoint] = Some(ServiceEndpoint(newHostName(), 8900))
        result mustBe expected
      })
    }

    "get endpoint from the topContributingKeysMap" in new HeimdallLoadBalancerMockContext(60, 60, true) {
      val loadBalancer: HeimdallLoadBalancer =
        new HeimdallLoadBalancer(
          nodeAndPerftrackAware,
          HeimdallLoadBalancerConfig(enableCache = true, reloadIntervalMs = 100))
      loadBalancer.start()
      nodeAndPerftrackAware.keySet.foreach(rtmVersion => {
        val result: Option[ServiceEndpoint]   = loadBalancer.getInstance(lastKey(rtmVersion), rtmVersion)
        val expected: Option[ServiceEndpoint] = Some(ServiceEndpoint(newHostName(60), 8900))
        result mustBe expected
      })
    }

    "return 002 endpoint from the cache" in new HeimdallLoadBalancerMockContext(2, 2, true, 0, 0) {
      val loadBalancer: HeimdallLoadBalancer =
        new HeimdallLoadBalancer(
          nodeAndPerftrackAware,
          HeimdallLoadBalancerConfig(enableCache = true, reloadIntervalMs = 100))
      loadBalancer.start()
      nodeAndPerftrackAware.keySet.foreach(rtmVersion => {
        val result: Option[ServiceEndpoint]   = loadBalancer.getInstance(someKey, rtmVersion)
        val expected: Option[ServiceEndpoint] = Some(ServiceEndpoint(newHostName(2), 8900))
        result mustBe expected
      })
    }

  }

}
