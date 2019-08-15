package services.zookeeper

import java.util

import org.apache.curator.framework.listen.ListenerContainer
import org.apache.curator.framework.recipes.cache.{ChildData, PathChildrenCache, PathChildrenCacheListener}

/**
  * PathChildrenCacheFacade is used to decouple from the PathChildrenCache implementation
  * to allow unit testing.
  */
private[zookeeper] trait PathChildrenCacheFacade {
  def getListenable: ListenerContainer[PathChildrenCacheListener]
  def start(mode: PathChildrenCache.StartMode): Unit
  def getCurrentData: java.util.List[ChildData]
  def close(): Unit
}

private[zookeeper] class PathChildrenCacheAdapter(impl: PathChildrenCache) extends PathChildrenCacheFacade {
  def getListenable: ListenerContainer[PathChildrenCacheListener] = impl.getListenable
  def start(mode: PathChildrenCache.StartMode): Unit              = impl.start(mode)
  def getCurrentData: util.List[ChildData]                        = impl.getCurrentData
  def close(): Unit                                               = impl.close()
}
