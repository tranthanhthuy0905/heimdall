package controllers

import java.util.UUID
import java.util.UUID.randomUUID

import actions.{HeimdallRequestAction, TokenValidationAction}
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.nimbusds.jwt.SignedJWT
import com.typesafe.config.Config
import models.auth.{AuthorizationData, JWTWrapper, StreamingSessionData, StreamingSessionDataImpl}
import models.common.{AuthorizationAttr, MediaIdent, MediaIdentAttr}
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.PlaySpec
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._
import services.audit.{AuditClient, AuditEvent}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class AuditControllerSpec extends PlaySpec with MockitoSugar with ScalaFutures {
  implicit val defaultPatience =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  trait MockContext {
    implicit val materializer = ActorMaterializer()(ActorSystem())

    final val fileId = UUID.fromString("84d4df17-48b5-474d-9786-43f7fce5a288")
    final val evidenceId =
      UUID.fromString("40ef08e4-c5ef-4e94-8bff-0241c7684130")
    final val partnerId =
      UUID.fromString("79879d6f-c04c-44df-a5f3-8739b81bc8a3")
    final val media: MediaIdent =
      new MediaIdent(List(fileId), List(evidenceId), partnerId)
    final val jwtString =
      "eyJraWQiOiI1ZjBlZTM4Y2YyYTdkZWVkY2Q5ZTFiNGFjZDYwYTQ3NSIsImFsZyI6IkhTMjU2In0.eyJzdWIiOiIxNDdkZDgzNi01ZGI3LTQwZDQtOTJhMS1jYTQ1NmMzMjVmOTIiLCJ2ZXIiOiIyIiwicm9sZXMiOltdLCJpc3MiOiJodHRwczpcL1wvYXV0aC5ldmlkZW5jZS5jb20iLCJzdWJfdHlwIjoiU3Vic2NyaWJlciIsImF1ZF90eXAiOiJQYXJ0bmVyIiwic3ViX2QiOiJmM2Q3MTliYy1kYjJiLTRiNzEtYmZiMS00MzYyNDBmYjkwOTkiLCJhdWQiOiJmM2Q3MTliYy1kYjJiLTRiNzEtYmZiMS00MzYyNDBmYjkwOTkiLCJzZCI6e30sIm5iZiI6MTU0ODE5OTE0NCwiYXVkX2QiOiJmM2Q3MTliYy1kYjJiLTRiNzEtYmZiMS00MzYyNDBmYjkwOTkiLCJzY29wZXMiOlsiZGV2aWNlLnJldHVybi5tb2RpZnkiXSwiZXhwIjoxNTQ4MjAwMDQ0LCJpYXQiOjE1NDgxOTkxNDQsImp0aSI6IjNjYjg0MzM4LThlYTYtNGFkMS1hZDg2LTEzYzU5MzAxYTQyYyJ9.JAyVxHmc1b6gAIQlaXja43hnvYQyiKk--MhpudOD-WI"
    final val cookie = "1f2txgyowbrcwu4yy8p8qr0p8fs1a4vk681aq5fiuwnympy054"
    final val streamingSessionToken =
      "wr3qabKbqYeI0j5Wo_ZObVINHioPj5GyTxhChEJgiw0-"
    final val secret: String = "testing-play-secret"
    final val authData = AuthorizationData(
      cookie,
      jwtString,
      JWTWrapper(SignedJWT.parse(jwtString))
    )
    final val happyUri =
      s"/media/streamed?file_id=$fileId&evidence_id=$evidenceId&streamingSessionToken=$streamingSessionToken&partner_id=$partnerId"

    val configMock: Config = mock[Config]
    when(configMock.getString("play.http.secret.key")) thenReturn secret

    val streamingSessionData: StreamingSessionData =
      new StreamingSessionDataImpl(configMock)

    val heimdallRequestAction = new HeimdallRequestAction(
      new BodyParsers.Default(PlayBodyParsers())
    )
    val tokenValidationAction = new TokenValidationAction(streamingSessionData)

    val controllerComponents = Helpers.stubControllerComponents(
      playBodyParsers = Helpers.stubPlayBodyParsers(materializer)
    )

    val mockAuditClient = mock[AuditClient]

    val controller = new AuditController(
      heimdallRequestAction,
      tokenValidationAction,
      mockAuditClient,
      controllerComponents
    )
  }

  "AuditController" should {

    "return 200 status" in new MockContext {
      when(
        mockAuditClient
          .recordEndSuccess(List[AuditEvent](ArgumentMatchers.any()))
      ).thenReturn(Future.successful(List(randomUUID.toString)))
      val fakeRequest = FakeRequest(GET, happyUri)
        .addAttr(AuthorizationAttr.Key, authData)
        .addAttr(MediaIdentAttr.Key, media)
      val res = controller.recordMediaStreamedEvent(fakeRequest)
      whenReady(res) { result =>
        result.header.status mustBe Results.Ok.header.status
      }
    }

    "return 403 when media identifiers are missing" in new MockContext {
      val fakeRequest =
        FakeRequest(GET, happyUri).addAttr(AuthorizationAttr.Key, authData)
      val res = controller.recordMediaStreamedEvent(fakeRequest)
      whenReady(res) { result =>
        result.header.status mustBe Results.Forbidden.header.status
      }
    }

    "return InternalServerError RuntimeException" in new MockContext {
      when(
        mockAuditClient
          .recordEndSuccess(List[AuditEvent](ArgumentMatchers.any()))
      ).thenThrow(new RuntimeException("Deliberate runtime exception"))
      val fakeRequest = FakeRequest(GET, happyUri)
        .addAttr(AuthorizationAttr.Key, authData)
        .addAttr(MediaIdentAttr.Key, media)
      val res = controller.recordMediaStreamedEvent(fakeRequest)
      whenReady(res) { result =>
        result.header.status mustBe Results.InternalServerError.header.status
      }
    }

    "return InternalServerError on failed recordEndSuccess" in new MockContext {
      when(
        mockAuditClient
          .recordEndSuccess(List[AuditEvent](ArgumentMatchers.any()))
      ).thenReturn(Future.failed(new Exception("Deliberate failure")))
      val fakeRequest = FakeRequest(GET, happyUri)
        .addAttr(AuthorizationAttr.Key, authData)
        .addAttr(MediaIdentAttr.Key, media)
      val res = controller.recordMediaStreamedEvent(fakeRequest)
      whenReady(res) { result =>
        result.header.status mustBe Results.InternalServerError.header.status
      }
    }

  }

}
