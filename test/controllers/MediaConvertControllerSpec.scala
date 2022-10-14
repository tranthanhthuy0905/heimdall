package controllers

import java.util.UUID
import java.util.UUID.randomUUID
import actions.{ApidaeRequestAction, AuditEventActionBuilder, HeimdallRequestAction, MediaConvertValidation, PermValidationActionBuilder, TokenValidationAction}
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.nimbusds.jwt.SignedJWT
import com.typesafe.config.Config
import junit.framework.TestCase.assertEquals
import models.auth.{AuthorizationData, JWTWrapper, StreamingSessionData, StreamingSessionDataImpl}
import models.common.{AuthorizationAttr, FileIdent, MediaIdent, MediaIdentAttr}
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.{verify, when, _}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.PlaySpec
import play.api.libs.ws.WSResponse
import play.api.mvc._
import play.api.test.Helpers._
import play.api.test._
import services.audit.{AuditClient, AuditEvent}
import services.sage.SageClient
import services.apidae.ApidaeClient
import services.pdp.PdpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future


class MediaConvertControllerSpec extends PlaySpec with MockitoSugar with ScalaFutures {
  implicit val defaultPatience =
    PatienceConfig(timeout = Span(5, Seconds), interval = Span(500, Millis))

  trait MockContext {

    implicit val materializer = ActorMaterializer()(ActorSystem())

    final val fileId = UUID.fromString("84d4df17-48b5-474d-9786-43f7fce5a288")
    final val evidenceId =
      UUID.fromString("40ef08e4-c5ef-4e94-8bff-0241c7684130")
    final val partnerId = {
      UUID.fromString("f3d719bc-db2b-4b71-bfb1-436240fb9099")
    }
    final val url = "https://goodtrouble.ag1.evidence.com/justice/cases/a43b12c6-c5ec-418a-90a9-f53363951d20/evidence/bd9c5519a44e4ad5963f54416d229288"
    final val emptyUUID = UUID.fromString("00000000-0000-0000-0000-000000000000")
    final val file: FileIdent = FileIdent(fileId, evidenceId, partnerId)
    final val media: MediaIdent =
      new MediaIdent(List(fileId), List(evidenceId), partnerId)

    final val jwtString =
      "eyJraWQiOiI1ZjBlZTM4Y2YyYTdkZWVkY2Q5ZTFiNGFjZDYwYTQ3NSIsImFsZyI6IkhTMjU2In0.eyJzdWIiOiIxNDdkZDgzNi01ZGI3LTQwZDQtOTJhMS1jYTQ1NmMzMjVmOTIiLCJ2ZXIiOiIyIiwicm9sZXMiOltdLCJpc3MiOiJodHRwczpcL1wvYXV0aC5ldmlkZW5jZS5jb20iLCJzdWJfdHlwIjoiU3Vic2NyaWJlciIsImF1ZF90eXAiOiJQYXJ0bmVyIiwic3ViX2QiOiJmM2Q3MTliYy1kYjJiLTRiNzEtYmZiMS00MzYyNDBmYjkwOTkiLCJhdWQiOiJmM2Q3MTliYy1kYjJiLTRiNzEtYmZiMS00MzYyNDBmYjkwOTkiLCJzZCI6e30sIm5iZiI6MTU0ODE5OTE0NCwiYXVkX2QiOiJmM2Q3MTliYy1kYjJiLTRiNzEtYmZiMS00MzYyNDBmYjkwOTkiLCJzY29wZXMiOlsiZGV2aWNlLnJldHVybi5tb2RpZnkiXSwiZXhwIjoxNTQ4MjAwMDQ0LCJpYXQiOjE1NDgxOTkxNDQsImp0aSI6IjNjYjg0MzM4LThlYTYtNGFkMS1hZDg2LTEzYzU5MzAxYTQyYyJ9.JAyVxHmc1b6gAIQlaXja43hnvYQyiKk--MhpudOD-WI"
    final val cookie = "1f2txgyowbrcwu4yy8p8qr0p8fs1a4vk681aq5fiuwnympy054"

    final val authData = AuthorizationData(
      cookie,
      jwtString,
      JWTWrapper(SignedJWT.parse(jwtString))
    )
    final val happyUriWithoutUrl =
      s"/media/video/convert?evidence_id=$evidenceId&partner_id=$partnerId&file_id=$fileId"

    final val happyUriWithUrl =
      s"/media/video/convert?evidence_id=$evidenceId&partner_id=$partnerId&file_id=$fileId&url=$url"

    val heimdallRequestAction = HeimdallRequestAction(
      new BodyParsers.Default(PlayBodyParsers())
    )

    val apidaeRequestAction = ApidaeRequestAction()

    val mockPdpClient = mock[PdpClient](RETURNS_SMART_NULLS)
    when(mockPdpClient.enforceBatch(any(), any() , any())) thenReturn(Future(true))

    val permValidation = PermValidationActionBuilder(mockPdpClient)

    val controllerComponents = Helpers.stubControllerComponents(
      playBodyParsers = Helpers.stubPlayBodyParsers(materializer)
    )

    val mediaConvertValidation = MediaConvertValidation()
    val auditEventAction = AuditEventActionBuilder()

    var mockAuditClient = mock[AuditClient](RETURNS_SMART_NULLS)
    when(
      mockAuditClient
        .recordEndSuccess(ArgumentMatchers.any[AuditEvent]())
    ) thenReturn(Future.successful(Right(randomUUID.toString)))

    var mockSageClient  = mock[SageClient](RETURNS_SMART_NULLS)

    val wsResponseMock = mock[WSResponse]
    when(wsResponseMock.status) thenReturn(200)

    var mockApidaeClient= mock[ApidaeClient](RETURNS_SMART_NULLS)
    when(mockApidaeClient.transcode(any[UUID], any[UUID], any[UUID], any[UUID], any[Option[String]] )) thenReturn(Future.successful(wsResponseMock))

    val controller = new MediaConvertController(
      heimdallRequestAction,
      permValidation,
      apidaeRequestAction,
      auditEventAction,
      mediaConvertValidation,
      mockApidaeClient,
      mockAuditClient,
      controllerComponents,
    )
  }

  "MediaConvertController" should {

    "pass all params excluding the optional url to apidae and return a 200" in new MockContext {
      val partnerIdCaptor: ArgumentCaptor[UUID] = ArgumentCaptor.forClass(classOf[UUID])
      val userIdCaptor: ArgumentCaptor[UUID] = ArgumentCaptor.forClass(classOf[UUID])
      val evidenceIdCaptor: ArgumentCaptor[UUID] = ArgumentCaptor.forClass(classOf[UUID])
      val fileIdCaptor: ArgumentCaptor[UUID] = ArgumentCaptor.forClass(classOf[UUID])
      val urlCaptor: ArgumentCaptor[Option[String]] = ArgumentCaptor.forClass(classOf[Option[String]])

      val fakeRequest = FakeRequest(POST, happyUriWithoutUrl)
        .addAttr(AuthorizationAttr.Key, authData)
        .addAttr(MediaIdentAttr.Key, media)

      val res = controller.convert(fakeRequest)
      whenReady(res) { result =>
        result.header.status mustBe Results.Ok.header.status
      }

      verify(mockApidaeClient).transcode(partnerIdCaptor.capture(), userIdCaptor.capture(), evidenceIdCaptor.capture(), fileIdCaptor.capture(), urlCaptor.capture())
      assertEquals(partnerId, partnerIdCaptor.getValue)
      assertEquals(fileId, fileIdCaptor.getValue)
      assertEquals(evidenceId, evidenceIdCaptor.getValue)
      assertEquals(None, urlCaptor.getValue)

    }

    "pass all params including the optional url (used for Justice) to apidae and return a 200" in new MockContext {
      val partnerIdCaptor: ArgumentCaptor[UUID] = ArgumentCaptor.forClass(classOf[UUID])
      val userIdCaptor: ArgumentCaptor[UUID] = ArgumentCaptor.forClass(classOf[UUID])
      val evidenceIdCaptor: ArgumentCaptor[UUID] = ArgumentCaptor.forClass(classOf[UUID])
      val fileIdCaptor: ArgumentCaptor[UUID] = ArgumentCaptor.forClass(classOf[UUID])
      val urlCaptor: ArgumentCaptor[Option[String]] = ArgumentCaptor.forClass(classOf[Option[String]])

      val fakeRequest = FakeRequest(POST, happyUriWithUrl)
        .addAttr(AuthorizationAttr.Key, authData)
        .addAttr(MediaIdentAttr.Key, media)

      val res = controller.convert(fakeRequest)
      whenReady(res) { result =>
        result.header.status mustBe Results.Ok.header.status
      }

      verify(mockApidaeClient).transcode(partnerIdCaptor.capture(), userIdCaptor.capture(), evidenceIdCaptor.capture(), fileIdCaptor.capture(), urlCaptor.capture())
      assertEquals(partnerId, partnerIdCaptor.getValue)
      assertEquals(fileId, fileIdCaptor.getValue)
      assertEquals(evidenceId, evidenceIdCaptor.getValue)
      assertEquals(Some(url), urlCaptor.getValue)
    }
  }
}
