package services.sage

import com.typesafe.config.Config
import io.grpc.{Channel, ManagedChannelBuilder, Metadata}
import java.util
import java.util.UUID
import java.util.concurrent.TimeUnit

import com.axon.sage.protos.common.common.{Error, RequestContext}
import com.axon.sage.protos.v1.evidence_video_service.{ConvertedFile, GetConvertedFilesResponse}
import com.axon.sage.protos.v1.query_service.{Entity, ReadResponse}
import models.common.HeimdallError

import scala.collection.JavaConverters._

trait SageClientHelper extends Retry {
  def buildChannel(host: String, port: Int, ssl: Boolean, keepAliveConfig: Config, retryConfig: RetryConfig): Channel = {
    val retry = buildRetryConfig(Seq(retryConfig))
    val builder = withKeepAlive(ManagedChannelBuilder.forAddress(host, port), keepAliveConfig)

    if (ssl)
      builder.useTransportSecurity()
        .enableRetry()
        .disableServiceConfigLookUp()
        .defaultServiceConfig(retry)
        .build
    else
      builder.usePlaintext()
        .enableRetry()
        .disableServiceConfigLookUp()
        .defaultServiceConfig(retry)
        .build
  }

  private def withKeepAlive[T <: ManagedChannelBuilder[T]](builder: ManagedChannelBuilder[T], keepAliveConfig: Config): ManagedChannelBuilder[T] = {
    if (keepAliveConfig != null) {
      builder
        .keepAliveTime(keepAliveConfig.getLong("time_in_second"), TimeUnit.SECONDS)
        .keepAliveWithoutCalls(keepAliveConfig.getBoolean("without_call"))
    } else {
      builder
    }
  }

  def requestContext = RequestContext(
    correlationId = UUID.randomUUID.toString,
    callingService = "heimdall"
  )

  def credential(secret: String): Metadata = {
    val header = new Metadata()
    header.put(Metadata.Key.of("secret", Metadata.ASCII_STRING_MARSHALLER), secret)
    header
  }

  def toEither(readResponse: ReadResponse): Either[HeimdallError, Seq[Entity]] = {
    readResponse match {
      case ReadResponse(Some(err), _, _) => Left(toHeimdallError(err))
      case resp => Right(resp.entities)
    }
  }

  def toEither(getConvertedFilesResponse: GetConvertedFilesResponse): Either[HeimdallError, scala.collection.immutable.Seq[ConvertedFile]] = {
    getConvertedFilesResponse match {
      case GetConvertedFilesResponse(Some(err), _) => Left(toHeimdallError(err))
      case resp => Right(resp.files.sortBy(_.index).to[scala.collection.immutable.Seq])
    }
  }

  def toHeimdallError(error: Error): HeimdallError =
    error.errorCode match {
      case Error.ErrorCode.VALIDATION_ERROR => HeimdallError(error.message, HeimdallError.ErrorCode.VALIDATION_ERROR)
      case _ => HeimdallError(error.message, HeimdallError.ErrorCode.INTERNAL_SERVER_ERROR)
    }
}

trait Retry {
  // sometimes there's hiccup lead to server returning `i.g.StatusRuntimeException: UNAVAILABLE: HTTP/2 error code: NO_ERROR`
  // we would want to retry those using gRPC retry mechanism
  // https://github.com/grpc/proposal/blob/master/A6-client-retries.md
  //   The initial retry attempt will occur at random(0, initialBackoff).
  //   In general, the nth attempt since the last
  //   server pushback response (if any), will occur at random(0, min(initialBackoff*backoffMultiplier**(n-1), maxBackoff))
  def buildRetryConfig(retryConfigs: Seq[RetryConfig]): util.Map[String, util.List[util.Map[String, Object]]] = {
    Map(
      "methodConfig" -> retryConfigs.map(_.toMethodConfig).asJava
    ).asJava
  }
}

case class RetryMethod(method: String, service: String)

case class RetryConfig(methods: Seq[RetryMethod],
                       protoPackage: String,
                       config: Option[util.Map[String, Any]] = None
                      ) {
  private final val defaultRetryPolicy: util.Map[String, Any] = Map(
    "maxAttempts" -> 3.0,
    "initialBackoff" -> "1s",
    "maxBackoff" -> "5s",
    "backoffMultiplier" -> 1.1,
    "retryableStatusCodes" -> Seq("UNAVAILABLE").asJava
  ).asJava

  def toMethodConfig: util.Map[String, Object] = {
    val names = methods.map(m => Map("service" -> s"$protoPackage.${m.service}", "method" -> m.method).asJava)

    Map(
      "name" -> names.asJava,
      "retryPolicy" -> config.getOrElse(defaultRetryPolicy)
    ).asJava
  }
}
