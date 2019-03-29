package controllers

import java.util.UUID.randomUUID

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.nimbusds.jwt.SignedJWT
import models.auth.{AuthorizationAttr, AuthorizationData, JWTWrapper}
import models.common.HeimdallActionBuilder
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._
import services.audit.{AuditClient, AuditEvent}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuditControllerSpec extends PlaySpec with MockitoSugar {

  trait MockContext {
    implicit val materializer = ActorMaterializer()(ActorSystem())

    final val happyRequestString = "/media/streamed?file_id=79aea236e59442ccae2c6cd95af937d8&evidence_id=2b111cf1-d42b-4edd-b0ed-1a5d63f81b23&partner_id=f3d719bc-db2b-4b71-bfb1-436240fb9099"
    final val jwtString = "eyJraWQiOiI1ZjBlZTM4Y2YyYTdkZWVkY2Q5ZTFiNGFjZDYwYTQ3NSIsImFsZyI6IkhTMjU2In0.eyJzdWIiOiIxNDdkZDgzNi01ZGI3LTQwZDQtOTJhMS1jYTQ1NmMzMjVmOTIiLCJ2ZXIiOiIyIiwicm9sZXMiOltdLCJpc3MiOiJodHRwczpcL1wvYXV0aC5ldmlkZW5jZS5jb20iLCJzdWJfdHlwIjoiU3Vic2NyaWJlciIsImF1ZF90eXAiOiJQYXJ0bmVyIiwic3ViX2QiOiJmM2Q3MTliYy1kYjJiLTRiNzEtYmZiMS00MzYyNDBmYjkwOTkiLCJhdWQiOiJmM2Q3MTliYy1kYjJiLTRiNzEtYmZiMS00MzYyNDBmYjkwOTkiLCJzZCI6e30sIm5iZiI6MTU0ODE5OTE0NCwiYXVkX2QiOiJmM2Q3MTliYy1kYjJiLTRiNzEtYmZiMS00MzYyNDBmYjkwOTkiLCJzY29wZXMiOlsiZGV2aWNlLnJldHVybi5tb2RpZnkiXSwiZXhwIjoxNTQ4MjAwMDQ0LCJpYXQiOjE1NDgxOTkxNDQsImp0aSI6IjNjYjg0MzM4LThlYTYtNGFkMS1hZDg2LTEzYzU5MzAxYTQyYyJ9.JAyVxHmc1b6gAIQlaXja43hnvYQyiKk--MhpudOD-WI"
    final val token = "1f2txgyowbrcwu4yy8p8qr0p8fs1a4vk681aq5fiuwnympy054"

    final val authData = AuthorizationData(JWTWrapper(SignedJWT.parse(jwtString)), token, jwtString)

    val heimdallActionBuilder = new HeimdallActionBuilder(new BodyParsers.Default(PlayBodyParsers()))
    val controllerComponents = Helpers.stubControllerComponents(playBodyParsers = Helpers.stubPlayBodyParsers(materializer))
    val mockAuditClient = mock[AuditClient]

    val controller = new AuditController(heimdallActionBuilder, mockAuditClient, controllerComponents)
  }

  "AuditController" should {

    "return 200 status" in new MockContext {
      when(mockAuditClient.recordEndSuccess(List[AuditEvent](ArgumentMatchers.any()))).thenReturn(Future.successful(List(randomUUID.toString)))
      val fakeRequest = FakeRequest(GET, happyRequestString).addAttr(AuthorizationAttr.Key, authData)
      val res = controller.recordMediaStreamedEvent(fakeRequest)
      res.map { result => result mustBe Results.Ok }
    }

    "throw java.util.NoSuchElementException" in new MockContext {
      val fakeRequest = FakeRequest(GET, happyRequestString)
      assertThrows[NoSuchElementException](controller.recordMediaStreamedEvent(fakeRequest))
    }

    "throw exception on RuntimeException" in new MockContext {
      when(mockAuditClient.recordEndSuccess(List[AuditEvent](ArgumentMatchers.any()))).thenThrow(new RuntimeException("Some deliberate runtime exception"))
      val fakeRequest = FakeRequest(GET, happyRequestString).addAttr(AuthorizationAttr.Key, authData)
      assertThrows[RuntimeException](controller.recordMediaStreamedEvent(fakeRequest))
    }

    "return InternalServerError on failed recordEndSuccess" in new MockContext {
      when(mockAuditClient.recordEndSuccess(List[AuditEvent](ArgumentMatchers.any()))).thenReturn(Future.failed(new Exception("Some deliberate failure")))
      val fakeRequest = FakeRequest(GET, happyRequestString).addAttr(AuthorizationAttr.Key, authData)
      val res = controller.recordMediaStreamedEvent(fakeRequest)
      res.map {result => result mustBe Results.InternalServerError }
    }

  }

}
