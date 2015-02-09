package io.github.kender.spray.eureka.client

import scala.concurrent._

import akka.actor.ActorSystem
import spray.client.pipelining._
import spray.http._
import spray.json._

import io.github.kender.spray.eureka.{DataCenterInfo, InstanceInfo}
import org.slf4j.LoggerFactory

/**
 * A client for managing a service instance with Eureka
 * @param config EurekaConfig
 * @param actorSystem ActorSystem
 */
class InstanceClient(config: EurekaConfig)(implicit actorSystem: ActorSystem) {

  import spray.httpx.SprayJsonSupport._

  import actorSystem.dispatcher
  import io.github.kender.spray.eureka.EurekaJsonProtocol._
  import io.github.kender.spray.eureka.client.Loggable._

  type InstanceId = String

  val logger = LoggerFactory.getLogger(classOf[InstanceClient])

  private implicit object RequestLogger extends Loggable[HttpRequest] {
    override def asLogMessage(it: HttpRequest): String = s"httpRequest $it"
  }

  private def pipeline: HttpRequest ⇒ Future[HttpResponse] = {
    sendReceive
  }

  /**
   * de-register the instance from Eureka
   * @return A future which completes when after the call is complete.
   */
  def deRegister(): Future[Unit] = {
    pipeline(Delete(s"${config.serverUrl}/v2/apps/${config.instance.appId}")).map(_ ⇒ Unit)
  }

  /**
   * register the instance with Eureka
   * @return A future containing the instance id which completes when after the call is complete.
   */
  def register(): Future[InstanceId] = {
    val registration = InstanceInfo(
      config.instance.hostName,
      config.instance.appId,
      config.instance.ipAddress,
      config.instance.vipAddress,
      config.instance.secureVipAddress,
      "UP",
      Some(config.instance.port),
      config.instance.securePort,
      config.instance.homePageUrl,
      config.instance.statusPageUrl,
      config.instance.healthCheckUrl,
      DataCenterInfo()
    )

    pipeline(debugIt(logger) {
      Post(
        s"${config.serverUrl}/v2/apps/${config.instance.appId}",
        JsObject("instance" → registration.toJson))
    }) map { _ ⇒ config.instance.hostName }
  }
}
