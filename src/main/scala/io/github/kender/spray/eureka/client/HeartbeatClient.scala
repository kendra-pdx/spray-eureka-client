package io.github.kender.spray.eureka.client

import scala.concurrent.Future
import scala.util.Try
import scala.util.control.NonFatal

import akka.actor.ActorSystem
import spray.client.pipelining._
import spray.http._

import org.slf4j.LoggerFactory

class HeartbeatClient(config: EurekaConfig)(implicit actorSystem: ActorSystem) {
  type HealthCheck = () ⇒ Try[Unit]
  type Pipeline = HttpRequest ⇒ Future[HttpResponse]

  implicit class TryHelpers[T](tried: Try[T]) {
    def onFailure(handler: Throwable ⇒ Unit) = {
      tried recoverWith { case NonFatal(t) ⇒
        handler(t)
        tried
      }
    }
  }

  import actorSystem.dispatcher

  val logger = LoggerFactory.getLogger(classOf[HeartbeatClient])

  def pipeline: Pipeline = sendReceive

  def start(healthCheck: HealthCheck, instanceId: String): Unit = {
    import actorSystem.dispatcher
    val heartbeat = actorSystem.scheduler.schedule(config.heartbeat.interval, config.heartbeat.interval) {
      healthCheck() map { _ ⇒
        logger.debug("sending heartbeat")
        val url = s"${config.serverUrl}/v2/apps/${config.instance.appId}/$instanceId"
        pipeline(Put(url)) map { response ⇒
          if (response.status.isFailure) {
            logger.error("heartbeat response: {}", response.status)
          }
        }
      } onFailure {
        logger.error("heartbeat failed", _)
      }
    }

    actorSystem.registerOnTermination {
      heartbeat.cancel()
    }
  }
}
