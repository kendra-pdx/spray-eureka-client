package io.github.kender.spray.eureka.client

import scala.util.Try

import akka.actor.ActorSystem
import spray.client.pipelining._
import spray.http._

import org.slf4j.LoggerFactory

class HeartbeatClient(config: EurekaConfig)(implicit actorSystem: ActorSystem) {

  import actorSystem.dispatcher

  val logger = LoggerFactory.getLogger(classOf[HeartbeatClient])

  def pipeline: HttpPipeline[HttpRequest, HttpResponse] = sendReceive

  def start(healthCheck: HealthCheck): Unit = {
    import actorSystem.dispatcher
    actorSystem.scheduler.schedule(config.heartbeat.interval, config.heartbeat.interval) {
      Try {
        logger.info("sending heartbeat")
      } onFailure {
        logger.error("heartbeat failed", _)
      }
    }
  }
}
