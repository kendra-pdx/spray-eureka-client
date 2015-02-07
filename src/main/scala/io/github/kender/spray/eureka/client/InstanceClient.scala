package io.github.kender.spray.eureka.client

import akka.actor.ActorSystem
import scala.concurrent._, Future._

class InstanceClient(config: EurekaConfig)(implicit actorSystem: ActorSystem) {
  def register(): Future[Instance] = successful {
    Instance("foo")
  }
}
