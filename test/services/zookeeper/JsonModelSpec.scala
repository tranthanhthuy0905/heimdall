package services.zookeeper

import com.evidence.service.common.zookeeper.ServiceEndpoint
import org.scalatestplus.play.PlaySpec
import play.api.libs.json.{JsResultException, JsValue, Json}

class JsonModelSpec extends PlaySpec {

  import ZookeeperPackageTestHelper._

  "JsonModel" should {

    "create RtmNodeInfo object using correct json string" in {
      val hostName = newHostName()
      val result   = Json.parse(newRtmNodeString(hostName)).as[RtmNodeInfo]
      val expected = RtmNodeInfo(ServiceEndpoint(hostName, 8900), "ALIVE")
      result mustBe expected
    }

    "return true for an alive RtmNodeInfo on isAlive call" in {
      val hostName = newHostName()
      val result   = Json.parse(newRtmNodeString(hostName)).as[RtmNodeInfo]
      result.isAlive mustBe true
    }

    "return false for not alive RtmNodeInfo on isAlive call" in {
      val hostName = newHostName()
      val result   = Json.parse(newRtmNodeString(hostName, "SOME-STATUS")).as[RtmNodeInfo]
      result.isAlive mustBe false
    }

    "throw JsResultException on incomplete RtmNodeInfo" in {
      val someJsonString = s"""{
                              |  "serviceEndpoint":{},
                              |  "additionalEndpoints":{},
                              |  "status":"ALIVE"
                              |}""".stripMargin

      assertThrows[JsResultException] {
        Json.parse(someJsonString).as[RtmNodeInfo]
      }
    }

    "create PerftrakDatum object using correct json value" in {
      val endpoint = ServiceEndpoint(newHostName(), 8900)
      val fileId1  = randomKey
      val fileId2  = randomKey
      val fileId3  = randomKey
      val json: JsValue =
        Json.parse(
          newPerftrakNodeString(Seq[SingleTop](SingleTop(fileId1, 1), SingleTop(fileId2, 1)), fileId3, 10, 100))
      val planeComputational = PerftrakModel.planeComputationalReads.reads(json)
      val planeCaching       = PerftrakModel.planeCachingReads.reads(json)
      val result             = PerftrakDatum(endpoint, planeComputational.asOpt, planeCaching.get)
      val expectedPlaneComp  = PlaneComputational(10, 100)
      val expectedPlaneCaching =
        PlaneCaching(Seq[SingleTop](SingleTop(fileId1, 1), SingleTop(fileId2, 1), SingleTop(fileId3, 1)))
      val expected = PerftrakDatum(endpoint, Some(expectedPlaneComp), Some(expectedPlaneCaching))
      result mustBe expected
    }

    "create PerftrakDatum with None objects using incomplete json value" in {
      val someJsonString = s"""{
                              |  "plane-caching":{
                              |    "component-aggregate":99614720,
                              |    "component-capacity":1150699300
                              |  },
                              |  "plane-computational":{
                              |  },
                              |  "plane-perf-penalty":{
                              |    "component-aggregate":0
                              |  }
                              |}""".stripMargin

      val endpoint           = ServiceEndpoint(newHostName(), 8900)
      val json: JsValue      = Json.parse(someJsonString)
      val planeComputational = PerftrakModel.planeComputationalReads.reads(json)
      val planeCaching       = PerftrakModel.planeCachingReads.reads(json)
      val result             = PerftrakDatum(endpoint, planeComputational.asOpt, planeCaching.get)
      val expected           = PerftrakDatum(endpoint, None, None)
      result mustBe expected
    }

  }
}
