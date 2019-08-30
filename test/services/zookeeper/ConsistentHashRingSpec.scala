package services.zookeeper

import java.util.UUID
import org.scalatest.FlatSpec

/**
  * Tests for [[services.zookeeper.ConsistentHashRing]]
  */
class ConsistentHashRingSpec extends FlatSpec {

  import ZookeeperPackageTestHelper._

  val replicaCount = 100
  val nodeCount    = 40
  val keyCount     = 1000000
  val keys         = randomKeys(keyCount)

  it should "^ distribute keys among nodes with deviation less than 40%" in {
    for (i <- 1 to 10) {
      val hash =
        new ConsistentHashRing[String](
          identity,
          replicaCount * i,
          (1 to nodeCount).map(n => s"http://tus1ue1lrtm00$n.us1.taservs.net:8900").toList)
      val wantedCount   = keyCount / nodeCount
      val nodesReturned = keys.map(key => (hash.get(key).get))
      val nodeCounts    = nodesReturned.groupBy(n => n).map(e => (e._1, e._2.length))
      val (minCount, maxCount) = nodeCounts.foldLeft((Int.MaxValue, Int.MinValue)) {
        case ((min, max), e) => (math.min(min, e._2), math.max(max, e._2))
      }
      val over  = 100.0 * (maxCount - wantedCount) / wantedCount
      val under = 100.0 * (wantedCount - minCount) / wantedCount

      println(
        s"[$i] distribution test variables: nodeCount=$nodeCount, keyCount=$keyCount, replicaCount=${replicaCount * i}")
      println(s"[$i] $wantedCount: desired key count per node")
      println(s"[$i] $maxCount: most data keys on one node, $over over")
      println(s"[$i] $minCount: least data keys on one node, $under under")

      assert(over < 40 && under < 40)
    }
  }

  it should "^ shuffle less than 3% of keys" in {
    for (i <- 1 to 10) {
      val hash1 =
        new ConsistentHashRing[String](
          identity,
          replicaCount * i,
          (1 to nodeCount).map(n => s"http://tus1ue1lrtm00$n.us1.taservs.net:8900").toList)
      val hash2 =
        new ConsistentHashRing[String](
          identity,
          replicaCount * i,
          (1 to nodeCount + 1).map(n => s"http://tus1ue1lrtm00$n.us1.taservs.net:8900").toList)
      val keysMoved = keys.map { key =>
        val nodeId1 = hash1.get(key).get
        val nodeId2 = hash2.get(key).get
        nodeId1 != nodeId2
      }.filter(_ == true).length
      val percentMoved = 100.0 * keysMoved / keyCount

      println(
        s"[$i] shuffle test variables: nodeCount=$nodeCount, keyCount=$keyCount, replicaCount=${replicaCount * i}")
      println(s"[$i] $keysMoved keys moved, $percentMoved")

      assert(percentMoved < 3)
    }
  }

  it should "distribute keys evenly across the things" in {

    val servers = (0 to 9).map(n => s"http://tus1ue1lrtm00$n.us1.taservs.net:8900").toList
    val hash    = new ConsistentHashRing[String](identity, 100, servers)

    val keys = for (_ <- 0 to 999) yield UUID.randomUUID().toString.replaceAll("-", "")
    val serverCounts = keys.foldLeft(Map.empty[String, Int]) {
      case (counts, key) =>
        val server = hash.get(key).get
        counts + (server -> (counts.getOrElse(server, 0) + 1))
    }

    servers.foreach { server =>
      val count = serverCounts.getOrElse(server, 0)
      assert(60 <= count && count <= 140, server)
    }
  }

  it should "keep most keys assigned to the same thing when the set of things change" in {

    val servers = (0 to 9).map(n => s"http://tus1ue1lrtm00$n.us1.taservs.net:8900").toList
    val keys    = (for (_ <- 0 to 999) yield UUID.randomUUID().toString.replaceAll("-", "")).toList

    val originalAssignments = assign(servers, keys)

    val removedServers     = servers diff List("http://tus1ue1lrtm002.us1.taservs.net:8900")
    val removedAssignments = assign(removedServers, keys)

    val (rSame, rDiff) = diff(originalAssignments, removedAssignments)
    assert(rSame >= 800)
    assert(rDiff <= 200)

    val addedServers     = removedServers ++ List("http://tus1ue1lrtm999.us1.taservs.net:8900")
    val addedAssignments = assign(addedServers, keys)

    val (aSame, aDiff) = diff(removedAssignments, addedAssignments)
    assert(aSame >= 800)
    assert(aDiff <= 200)
  }

  it should "assign about N times more keys to bigServer than to smallServer" in {
    val baseNumberOfReplicas = 100
    val N                    = 5
    val tolerance            = 0.3
    val bigServer            = "http://tus1ue1lrtm001.us1.taservs.net:8900"
    val smallServer          = "http://tus1ue1lrtm002.us1.taservs.net:8900"

    val replicasMap = Map[String, Int](bigServer -> baseNumberOfReplicas * N, smallServer -> baseNumberOfReplicas)
    val hash        = new ConsistentHashRing[String](identity, replicasMap)

    var sumOfRatio      = 0.0
    var minRatio        = N * 100.0
    var maxRatio        = 0.0
    val numOfIterations = 1000
    val keyNumber       = 10000
    for (_ <- 1 to numOfIterations) {
      val keys = for (_ <- 1 to keyNumber) yield UUID.randomUUID().toString.replaceAll("-", "")

      val serverCounts: Map[String, Int] = keys.foldLeft(Map.empty[String, Int]) {
        case (counts, key) =>
          val server = hash.get(key).get
          counts + (server -> (counts.getOrElse(server, 0) + 1))
      }

      val bigServerCount   = serverCounts.get(bigServer).getOrElse(0)
      val smallServerCount = serverCounts.get(smallServer).getOrElse(0)
      assert(bigServerCount != 0 && smallServerCount != 0)

      val resultRatio = bigServerCount.toFloat / smallServerCount.toFloat

      sumOfRatio += resultRatio
      if (resultRatio > maxRatio)
        maxRatio = resultRatio
      if (resultRatio < minRatio)
        minRatio = resultRatio

      assert(resultRatio > (N.toFloat - N * tolerance) && resultRatio < (N.toFloat + N * tolerance))
    }

    val averageRatio = sumOfRatio / numOfIterations
    printf(
      ">>> With N = %s and key number = %s on %s test repetitions averageRatio = %s, minRatio = %s, maxRatio = %s\n",
      N,
      keyNumber,
      numOfIterations,
      averageRatio,
      minRatio,
      maxRatio
    )

    assert(averageRatio > (averageRatio - N * tolerance) && averageRatio < (averageRatio + N * tolerance))
  }

  it should "assign more keys to bigServer than other servers" in {
    val numberOfServers      = 10
    val r                    = scala.util.Random
    val bigServerNumber      = r.nextInt(numberOfServers)
    val bigServer            = s"http://tus1ue1lrtm00$bigServerNumber.us1.taservs.net:8900"
    val baseNumberOfReplicas = 100
    val N                    = 2
    val tolerance            = 0.3

    val servers: Map[String, Int] =
      (1 to numberOfServers).map(n => s"http://tus1ue1lrtm00$n.us1.taservs.net:8900" -> baseNumberOfReplicas).toMap
    val replicasMap = servers - bigServer + (bigServer -> baseNumberOfReplicas * N)
    val hash        = new ConsistentHashRing[String](identity, replicasMap)

    val numOfIterations = 10
    val keyNumber       = 10000
    for (_ <- 1 to numOfIterations) {
      val keys = for (_ <- 1 to keyNumber) yield UUID.randomUUID().toString.replaceAll("-", "")

      val serverCounts: Map[String, Int] = keys.foldLeft(Map.empty[String, Int]) {
        case (counts, key) =>
          val server = hash.get(key).get
          counts + (server -> (counts.getOrElse(server, 0) + 1))
      }

      val bigServerCount = serverCounts.get(bigServer).getOrElse(0)
      assert(bigServerCount > 0)

      // Number of keys on the "big" server must be always greater than anywhere else.
      replicasMap.foreach { server =>
        val count = serverCounts.getOrElse(server._1, 0)
        if (server._1 != bigServer) {
          assert(count < bigServerCount)
        }
      }

      val averageKeyCount  = (serverCounts.foldLeft(0)(_ + _._2) - bigServerCount) / (numberOfServers - 1)
      val expectedKeyCount = (keyNumber - bigServerCount) / (numberOfServers - 1)

      assert(averageKeyCount > 0)
      assert(
        averageKeyCount > (expectedKeyCount - expectedKeyCount * tolerance) && averageKeyCount < (expectedKeyCount + expectedKeyCount * tolerance))
    }
  }

  private def assign(servers: List[String], keys: List[String]): Map[String, String] = {
    val hash = new ConsistentHashRing[String](identity, 100, servers)
    keys.map(key => (key, hash.get(key).get)).toMap
  }

  private def diff[K, V](m1: Map[K, V], m2: Map[K, V]): (Int, Int) = {
    m1.foldLeft((0, 0)) {
      case ((same, differs), (m1Key, m1Value)) =>
        val isSame = m2.get(m1Key).contains(m1Value)
        if (isSame) (same + 1, differs) else (same, differs + 1)
    }
  }
}
