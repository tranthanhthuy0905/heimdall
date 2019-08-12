package services.zookeeper

import java.util.UUID

import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.zookeeper.{ConsistentHashLb, ServiceEndpoint, ZookeeperBuilder, ZookeeperConfig}
import com.google.inject.{Inject, Provider}
import com.typesafe.config.Config
import javax.inject.Singleton
import org.apache.curator.framework.CuratorFramework

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class ZookeeperClientProvider @Inject()(config: Config) extends Provider[CuratorFramework] {
  def get(): CuratorFramework = ZookeeperBuilder.connect(new ZookeeperConfig(config))
}

@Singleton
class ZookeeperServerSet @Inject()(lb: String => Option[ServiceEndpoint]) extends LazyLogging {

  def getInstance(key: String): Try[ServiceEndpoint] = {
    Try(lb(key)) match {
      case Success(Some(server)) => Try(server)
      case Success(None) =>
        logger.error("noServiceInstanceFound")("key" -> key)
        Failure(new Exception(s"No service instances found for key=$key"))
      case Failure(e) =>
        logger.error("failedToGetAnInstance")("key" -> key, "error" -> e.getMessage)
        Failure(e)
    }
  }

  // TODO: change the method signature to receive request instead of fileId to be more uniform
  def getInstanceAsFuture(fileId: UUID): Future[ServiceEndpoint] = {
    getInstance(fileId.toString) match {
      case Success(server)    => Future.successful(server)
      case Failure(exception) => Future.failed(exception)
    }
  }
}

object ZookeeperServerSet {

  def apply(basePath: String, zookeeper: CuratorFramework): ZookeeperServerSet = {
    def hashBase(e: ServiceEndpoint) = s"https://${e.host}:${e.port}/"
    val hash                         = ConsistentHashLb.apply(zookeeper, basePath, hashBase)
    val lb                           = hash.getServer _
    val serverSet                    = new ZookeeperServerSet(lb)
    serverSet
  }
}

class ZookeeperServerSetProvider @Inject()(zookeeper: CuratorFramework) extends Provider[ZookeeperServerSet] {
  def get(): ZookeeperServerSet = ZookeeperServerSet("/service/rtm/http", zookeeper)
}
