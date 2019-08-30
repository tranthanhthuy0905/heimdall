package services.zookeeper

import scala.collection.SortedMap
import java.security.MessageDigest

/**
  * Maps resources to a set of [[T]] elements. The idea is that it distributes the resources evenly and if only some
  * elements change over time most of the resource mappings will remain the same. Because of this property this can be
  * used to implement load balancing. (The resources are the things served by the servers and the elements are the
  * servers.)
  *
  * ConsistentHashRing was copied from https://git.taservs.net/ecom/service-common/blob/master/service-common/src/main/scala/com/evidence/service/common/ConsistentHash.scala
  * The difference is that it uses MD5 hash function instead of Murmur2 as ConsistentHash from service-common does.
  *
  * @param stringer converts the given [[T]] element to a string.
  * @param replicaCounts number of replicas per node.
  */
class ConsistentHashRing[T](stringer: T => String, replicaCounts: Map[T, Int]) {
  val md: MessageDigest = MessageDigest.getInstance("MD5")
  val circle: SortedMap[Long, T] = init(replicaCounts)
  val keys: Array[Long]          = circle.keys.toArray

  /**
    * @param numReplicas how many times each element will be present in the wheeled hash.
    * @param elems the elements in the hash
    */
  def this(stringer: T => String, numReplicas: Int, elems: List[T]) {
    this(stringer, Map(elems map { v =>
      (v, numReplicas)
    }: _*))
  }

  def this(stringer: T => String) {
    this(stringer, 100, Nil)
  }

  def init(replicaCounts: Map[T, Int]): SortedMap[Long, T] = {
    val replicasMap = replicaCounts.map({
      case (k, v) =>
        replicate(k, v)
    })
    val flattenedReplicasMap = replicasMap.flatten
    SortedMap(flattenedReplicasMap.toArray: _*)
  }

  /**
    * Gets the element associated with the specified resource by the hash.
    *
    * @return The element for the given resource, None if not available.
    */
  def get(resource: String): Option[T] = {
    val h     = md5(resource)
    val first = firstGe(keys, h)
    if (first < keys.length) {
      val numericKey = keys(first)
      circle.get(numericKey)
    } else {
      None
    }
  }

  /**
    * @return true if there are no elements in the hash, false otherwise.
    */
  def isEmpty: Boolean = circle.isEmpty

  def replicate(node: T, replicaCount: Int): Seq[(Long, T)] = {
    for (i <- 1 to replicaCount) yield hashNode(node, i) -> node
  }

  def hashNode(node: T, i: Int): Long = {
    md5(s"${stringer(node)}$i")
  }

  def md5(s: String): Long = {
    md.reset()
    val bigInt = BigInt(1, md.digest(s.getBytes))
    bigInt.toLong & 0x00000000ffffffffL
  }

  /**
    * return the index of first item that >= val.
    * not exist, return 0;
    * should be ordered array.
    */
  private def firstGe(arr: Array[Long], value: Long): Int = {
    var begin = 0
    var end   = arr.length - 1

    if (arr(end) < value || arr(0) > value) {
      0
    } else {

      var mid = begin
      while (end - begin > 1) {
        mid = (end + begin) / 2
        if (arr(mid) >= value) {
          end = mid
        } else {
          begin = mid
        }
      }

      if (arr(begin) > value || arr(end) < value) {
        throw new Exception("should not happen")
      }
      end
    }
  }
}
