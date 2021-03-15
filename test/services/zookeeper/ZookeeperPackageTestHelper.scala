package services.zookeeper

import java.util
import java.util.UUID

import org.apache.curator.framework.recipes.cache.ChildData
import org.apache.zookeeper.data.Stat
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.json.{JsValue, Json}

import scala.collection.JavaConverters._
import scala.io.Source

private[zookeeper] object ZookeeperPackageTestHelper {
  val rand            = new scala.util.Random(DateTime.now(DateTimeZone.UTC).getMillis)
  val someKey: String = "84d4df1748b5474d978643f7fce5a288"

  def randomKey: String = {
    UUID.randomUUID().toString.replace("-", "")
  }

  def randomKeys(keyCount: Int) = {
    for (_ <- 1 to keyCount) yield UUID.randomUUID().toString.replaceAll("-", "")
  }

  def intInRange(start: Int, end: Int): Int = {
    start + rand.nextInt((end - start) + 1)
  }

  def longInRange(start: Int = 1, end: Int = Int.MaxValue): Long = {
    (start + rand.nextInt((end - start) + 1)).toLong
  }

  def newStat(perftrakData: String): Stat = new Stat(
    longInRange(), /** long czxid */
    longInRange(), /** long mzxid */
    longInRange(), /** long ctime */
    longInRange(), /** long mtime */
    intInRange(0, 10000), /** int version */
    0, /** int cversion */
    0, /** int aversion */
    longInRange(), /** long ephemeralOwner */
    toByte(perftrakData).length, /** int dataLength */
    0, /** int numChildren */
    longInRange() /** long pzxid */
  )

  def newChildData(path: String, data: String): ChildData = {
    new ChildData(path, newStat(data), toByte(data))
  }

  def newHostName(n: Int = 1): String = {
    s"qus1uw2lrtm00$n.taservs.net"
  }

  def toByte(s: String): Array[Byte] = {
    s.toCharArray.map(_.toByte)
  }

  def newRandomSingleTops(n: Int): Seq[SingleTop] = {
    (1 to n).map(_ => SingleTop(randomKey, 1))
  }

  /**
    * RTM related utils.
    */

  def newRtmChildDataPath: String = {
    s"/service/rtm/http/member_00000000${intInRange(10, 99)}"
  }

  def newRtmNodeString(hostName: String = newHostName(), status: String = "ALIVE"): String = {
    s"""{
       |  "serviceEndpoint":{
       |    "host":"$hostName",
       |    "port":8900
       |  },
       |  "additionalEndpoints":{},
       |  "status":"$status"
       |}""".stripMargin
  }

  def newListOfRtmChildData(n: Int): util.List[ChildData] = {
    val children =
      (1 to n).map(e => newHostName(e)).map(hn => newChildData(newRtmChildDataPath, newRtmNodeString(hn))).asJava
    children
  }

  /**
    * Perftrak related utils.
    */

  def newPerftrakChildDataPath(hostName: String = newHostName()): String = {
    s"/service/rtm/http/perftrak/$hostName:8900"
  }

  def newPerftrakNodeString(
    tops: Seq[SingleTop] = Seq[SingleTop](),
    lastTop: String = someKey,
    agg: Double = 0.1): String = {
    val jsonTops = tops.foldLeft("")((m, e) => m + s"""{"k":"${e.k}","v":1},""")
    s"""{
       |  "plane-caching":{
       |    "component-tops":[
       |    $jsonTops{
       |      "k":"$lastTop","v":1
       |    }]
       |  },
       |  "plane-computational":{
       |    "component-aggregate":$agg
       |  }
       |}""".stripMargin
  }

  def trim(str: String): String = {
    val trimPattern          = """\"(.+)\"""".r
    val trimPattern(trimmed) = str
    trimmed
  }

  def getKeysFromSplunkExport(filename: String): List[String] = {
    val source: String      = Source.fromFile(filename).getLines.mkString
    val splunkJson: JsValue = Json.parse(source)
    val keysJson            = splunkJson \\ "file_id"
    val keys                = keysJson.toList.map(k => trim(k.toString().replace("-", "")))
    keys
  }

  def getChildDataFromSplunkExport(filename: String): (util.List[ChildData], util.List[ChildData]) = {
    val source: String      = Source.fromFile(filename).getLines.mkString
    val splunkJson: JsValue = Json.parse(source)
    val hosts               = splunkJson \\ "host"
    val snapshots           = splunkJson \\ "_raw"
    val valueTuples         = hosts zip snapshots
    val perftrakChildren = valueTuples map { t =>
      newChildData(newPerftrakChildDataPath(newHostNameFromJsValue(t._1)), newPerftrakDataFromJsValue(t._2))
    }
    val rtmNodes = hosts map { h =>
      newChildData(newRtmChildDataPath, newRtmNodeString(newHostNameFromJsValue(h)))
    }
    (rtmNodes.asJava, perftrakChildren.asJava)
  }

  def newHostNameFromJsValue(host: JsValue): String = {
    s"${trim(host.toString())}.taservs.net"
  }

  def newPerftrakDataFromJsValue(snapshotRaw: JsValue): String = {
    val snapshotPattern           = """.+snapshot=(.+) Current perftrak.*""".r
    val snapshotPattern(snapshot) = snapshotRaw.toString()
    val cleanSnapshot: String     = snapshot.toString().replaceAll("\\\\\"", "\"")
    val result                    = trim(cleanSnapshot.toString())
    result
  }

  def newListOfPerftrakChildData(
    n: Int,
    minAgg: Int = 1,
    maxAgg: Int = 10,
    lastKey: String = someKey): util.List[ChildData] = {
    val children: Seq[ChildData] =
      (1 until n)
        .map(e => newHostName(e))
        .map(
          hn =>
            newChildData(
              newPerftrakChildDataPath(hn),
              newPerftrakNodeString(newRandomSingleTops(5), randomKey, intInRange(minAgg, maxAgg))))

    if (n > 0 && !lastKey.isEmpty) {
      val lastChild = newChildData(
        newPerftrakChildDataPath(newHostName(n)),
        newPerftrakNodeString(newRandomSingleTops(5), lastKey, intInRange(minAgg, maxAgg).toDouble))
      List.concat(children, List(lastChild)).asJava
    } else {
      children.asJava
    }
  }

}
