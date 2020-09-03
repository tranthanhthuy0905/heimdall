package services.zookeeper

import org.apache.curator.framework.listen.ListenerContainer
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import org.mockito.Mockito._

import scala.collection.immutable

class HeimdallLoadBalancerUK2Spec extends PlaySpec with MockitoSugar {

  import ZookeeperPackageTestHelper._

  val rand = new scala.util.Random(DateTime.now(DateTimeZone.UTC).getMillis)

  private class HeimdallLoadBalancerMockUK2 {

    /**
      * The assumption is that consistent hash ring should distribute new requests corresponding to replica counts.
      * Old cached files will go to the endpoints as the map directs them too,
      * this will update the priority value and readjust the hash ring.
      */
    final val enableCache = false

    val rtmCacheMock          = newCacheMock
    val perftrakCacheMock     = newCacheMock
    val nodeAndPerftrackAware = Map(1 -> (rtmCacheMock, perftrakCacheMock))

    private def newCacheMock: PathChildrenCacheFacade = {
      val cacheMock         = mock[PathChildrenCacheFacade]
      val listenerContainer = new ListenerContainer[PathChildrenCacheListener]
      when(cacheMock.getListenable) thenReturn listenerContainer
      cacheMock
    }

    val (rtmData, perftrakData) = getChildDataFromSplunkExport("assets/uk2_perftrak_20190823_122033_122045.json")

    when(rtmCacheMock.getCurrentData) thenReturn rtmData
    when(perftrakCacheMock.getCurrentData) thenReturn perftrakData
  }

  "HeimdallLoadBalancerUK2Spec" should {
    "create load balancer with data exported from UK2, and engage all nodes" in new HeimdallLoadBalancerMockUK2 {
      val loadBalancer: HeimdallLoadBalancer =
        new HeimdallLoadBalancer(
          nodeAndPerftrackAware,
          HeimdallLoadBalancerConfig(enableCache = enableCache, reloadIntervalMs = 100, enableRTMv2 = false))

      loadBalancer.start()

      val keys          = randomKeys(1000000)
      val endpoints     = keys.map(k => loadBalancer.getInstance(k,1))
      val replicaCounts = loadBalancer.getReplicaCounts(1)
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
      println(
        s"calculated replicaCounts:\n $replicaCounts\nkeys distribution:\n $returnedEndpointCounts\ndeviations:\n $deviations")
      assert(deviations.size == replicaCounts.size)
    }

    "maintain time required to perform getInstance call under 0.01 ms" in new HeimdallLoadBalancerMockUK2 {
      val loadBalancer: HeimdallLoadBalancer =
        new HeimdallLoadBalancer(
          nodeAndPerftrackAware,
          HeimdallLoadBalancerConfig(enableCache = enableCache, reloadIntervalMs = 100, enableRTMv2 = false))
      loadBalancer.start()

      val keyCount                           = 10000000
      val keys: immutable.IndexedSeq[String] = randomKeys(keyCount)
      val t0: Long                           = System.nanoTime()
      keys.map(k => loadBalancer.getInstance(k, 1))
      val t1: Long               = System.nanoTime()
      val totalElapsedMs: Double = (t1 - t0).toDouble / 1000000.0
      val oneCallNs: Double      = (t1 - t0).toDouble / keyCount.toDouble

      println(s"getInstance performance test keys count=$keyCount")
      println(s"$totalElapsedMs milliseconds: elapsed time for")
      println(s"$oneCallNs nanoseconds: took for get instance call")

      assert(oneCallNs < 10000)
    }

    "maintain time required to perform reload + getInstance under 0.01 ms" in new HeimdallLoadBalancerMockUK2 {
      val reloadMs = 100
      val loadBalancer: HeimdallLoadBalancer =
        new HeimdallLoadBalancer(
          nodeAndPerftrackAware,
          HeimdallLoadBalancerConfig(enableCache = enableCache, reloadIntervalMs = reloadMs, enableRTMv2 = false))
      loadBalancer.start()

      val iterationsNumber = 10000000
      val key: String      = randomKey
      val t0: Long         = System.nanoTime()
      for (_ <- 1 to iterationsNumber) {
        loadBalancer.reload(1)
        loadBalancer.getInstance(key.toString,1)
      }
      val t1: Long               = System.nanoTime()
      val totalElapsedMs: Double = (t1 - t0).toDouble / 1000000.0
      val oneCycleNs: Double     = (t1 - t0).toDouble / iterationsNumber.toDouble

      println(s"reload + getInstance performance iterations number=$iterationsNumber with reload interval $reloadMs ms")
      println(s"$totalElapsedMs milliseconds: elapsed time for")
      println(s"$oneCycleNs nanoseconds: took to reload  load balancer and get node")

      assert(oneCycleNs < 10000)
    }

    "maintain time required to perform reload under 0.001 ms" in new HeimdallLoadBalancerMockUK2 {
      val reloadMs = 100
      val loadBalancer: HeimdallLoadBalancer =
        new HeimdallLoadBalancer(
          nodeAndPerftrackAware,
          HeimdallLoadBalancerConfig(enableCache = enableCache, reloadIntervalMs = reloadMs, enableRTMv2 = false))
      loadBalancer.start()

      val iterationsNumber = 10000000
      val t0: Long         = System.nanoTime()
      for (_ <- 1 to iterationsNumber) {
        loadBalancer.reload(1)
      }
      val t1: Long               = System.nanoTime()
      val totalElapsedMs: Double = (t1 - t0).toDouble / 1000000.0
      val oneCycleNs: Double     = (t1 - t0).toDouble / iterationsNumber.toDouble

      println(s"reload performance test iterations number=$iterationsNumber with reload interval of $reloadMs ms")
      println(s"$totalElapsedMs milliseconds: elapsed time for")
      println(s"$oneCycleNs nanoseconds: average time spent in reload call")

      assert(oneCycleNs < 1000)
    }
  }
}
