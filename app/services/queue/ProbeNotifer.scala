package services.queue

import com.evidence.service.common.logging.LazyLogging
import com.evidence.service.common.queue2.{ConcurrentMessageProducer, QueueFactory, RoundRobinSessionFactorySessionFactory}
import com.typesafe.config.Config
import javax.inject.{Inject, Provider}

class ProbeNotifier(producer: ConcurrentMessageProducer, messageSerializer: MessageSerializer, enable: Boolean) {
  def send(event: EventMessage): Unit = enqueue(messageSerializer.serialize(event))

  private def enqueue(message: String): Unit = {
    if (enable) producer.send(message)
  }
}

object ProbeNotifier extends LazyLogging{
  def apply(config: Config): ProbeNotifier = {
    val factory = RoundRobinSessionFactorySessionFactory(
      (1 to 1).map(_ => QueueFactory.newQueue("probe_video", config))
    )
    val producer = factory.newProducer()
    new ProbeNotifier(producer, JsonMessageSerializer, shouldNotify(config))
  }

  private def shouldNotify(config: Config): Boolean = {
    config.getBoolean("edc.queue.enable_probe_notifier") &&
      config.hasPath("edc.queue.probe_video")
  }
}

class ProbeNotifierProvider @Inject()(config: Config) extends Provider[ProbeNotifier] {
  def get(): ProbeNotifier = ProbeNotifier(config)
}
