package services.zookeeper

import com.evidence.service.common.zookeeper.{ZookeeperBuilder, ZookeeperConfig}
import com.google.inject.{Inject, Provider}
import com.typesafe.config.Config
import org.apache.curator.framework.CuratorFramework

class ZookeeperClientProvider @Inject()(config: Config) extends Provider[CuratorFramework] {
  def get(): CuratorFramework = ZookeeperBuilder.connect(new ZookeeperConfig(config))
}
