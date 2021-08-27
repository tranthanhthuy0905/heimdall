package services.sage

import com.axon.sage.protos.v1.evidence_service._
import com.typesafe.config.Config
import io.grpc.{Channel, CallCredentials, ManagedChannel, ManagedChannelBuilder}
import java.util
import java.util.concurrent.{Executor, TimeUnit}
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
