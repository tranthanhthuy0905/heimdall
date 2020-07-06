package services.pdp

import java.util

import com.axon.pdp.protos.v1.pdp_service.{PdpServiceGrpc, Tid => PTid}
import com.evidence.service.common.Tid
import io.grpc.CallCredentials.{MetadataApplier, RequestInfo}
import io.grpc.{CallCredentials, ManagedChannel, ManagedChannelBuilder, Metadata}

import scala.collection.JavaConverters._

trait PdpClientHelper extends Retry {
  def buildPdpClient(host: String, port: Int, secret: String): PdpServiceGrpc.PdpServiceStub = {
    val retryConfig: util.Map[String, util.List[util.Map[String, Object]]] =
      withRetryConfig("com.axon.sage.protos.v1.PdpService", Seq("enforce", "enforceBatch"))

    val channel = ManagedChannelBuilder
      .forAddress(host, port)
      .usePlaintext()
      //.useTransportSecurity()
      .enableRetry()
      .disableServiceConfigLookUp()
      .defaultServiceConfig(retryConfig)
      .build

    com.axon.pdp.protos.v1.pdp_service.PdpServiceGrpc
      .stub(channel)
      .withCallCredentials(credential(secret))
  }

  // if a secret is configured then put it in gRPC header for each call
  // it's will be used for authentication like thrift secret
  private def credential(secret: String): CallCredentials = {
    val secretKey: Metadata.Key[String] =
      Metadata.Key.of("secret", Metadata.ASCII_STRING_MARSHALLER)
    val headers = new Metadata()
    headers.put(secretKey, secret)

    new CallCredentials {
      override def applyRequestMetadata(
         requestInfo: RequestInfo,
         appExecutor: java.util.concurrent.Executor,
         applier: MetadataApplier
      ): Unit = {
        appExecutor.execute(new Runnable {
          override def run(): Unit = {
            applier.apply(headers)
          }
        })
      }
      override def thisUsesUnstableApi(): Unit = ()
    }
  }

  def toProtoTid(tid: Tid): Option[PTid] = tid.domain.map(d => PTid(tid.entity, tid.id.toString, d.toString))
  def toProtoTid(entity: String, id: String, domain: String): PTid = PTid(entity, id, domain)
}

trait Retry {
  // sometimes there's hiccup lead to server returning `i.g.StatusRuntimeException: UNAVAILABLE: HTTP/2 error code: NO_ERROR`
  // we would want to retry those using gRPC retry mechanism
  // https://github.com/grpc/proposal/blob/master/A6-client-retries.md
  //   The initial retry attempt will occur at random(0, initialBackoff).
  //   In general, the nth attempt since the last
  //   server pushback response (if any), will occur at random(0, min(initialBackoff*backoffMultiplier**(n-1), maxBackoff))
  val retryPolicy: util.Map[String, Any] = Map(
    "maxAttempts" -> 3.0,
    "initialBackoff" -> "1s",
    "maxBackoff" -> "5s",
    "backoffMultiplier" -> 1.1,
    "retryableStatusCodes" -> Seq("UNAVAILABLE").asJava
  ).asJava

  def withRetryConfig(serviceName: String, methodNames: Seq[String]) = {
    val names: Seq[util.Map[String, String]] = methodNames.map(
      (name: String) => Map("service" -> serviceName, "method" -> name).asJava
    )
    val methodConfig: util.Map[String, Object] =
      Map("name" -> names.asJava, "retryPolicy" -> retryPolicy).asJava

    Map("methodConfig" -> Seq(methodConfig).asJava).asJava
  }
}