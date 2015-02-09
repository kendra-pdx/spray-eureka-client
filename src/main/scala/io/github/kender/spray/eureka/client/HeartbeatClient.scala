package io.github.kender.spray.eureka.client

import scala.concurrent.Future
import scala.util.Try
import scala.util.control.NonFatal

import akka.actor.ActorSystem
import spray.client.pipelining._
import spray.http._

import org.slf4j.LoggerFactory

/**
 * This client periodically sends heartbeats to Eureka for the registered instance.
 * @param config EurekaConfig
 * @param actorSystem ActorSystem
 */
class HeartbeatClient(config: EurekaConfig)(implicit actorSystem: ActorSystem) {
  private type HealthCheck = () ⇒ Try[Unit]
  private type Pipeline = HttpRequest ⇒ Future[HttpResponse]

  private implicit class TryHelpers[T](tried: Try[T]) {
    def onFailure(handler: Throwable ⇒ Unit) = {
      tried recoverWith { case NonFatal(t) ⇒
        handler(t)
        tried
      }
    }
  }

  import actorSystem.dispatcher

  private val logger = LoggerFactory.getLogger(classOf[HeartbeatClient])

  private def pipeline: Pipeline = sendReceive

  /**
   * Begin sending heartbeats on the configured interval
   * @param healthCheck () ⇒ Try[Unit], on success, send the heartbeat
   * @param instanceId The instance id
   */
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
      // when the actor system shuts down, stop sending heartbeats
      heartbeat.cancel()
    }
  }
}
