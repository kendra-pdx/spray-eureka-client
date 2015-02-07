package io.github.kender.spray.eureka.client

import scala.concurrent._

import akka.actor.ActorSystem
import spray.client.pipelining._
import spray.http._
import spray.json._

import io.github.kender.spray.eureka.{DataCenterInfo, Registration}
import org.slf4j.LoggerFactory

class InstanceClient(config: EurekaConfig)(implicit actorSystem: ActorSystem) {

  import spray.httpx.SprayJsonSupport._

  import actorSystem.dispatcher
  import io.github.kender.spray.eureka.EurekaJsonProtocol._
  import io.github.kender.spray.eureka.client.Loggable._

  type Pipeline = HttpRequest ⇒ Future[HttpResponse]

  val logger = LoggerFactory.getLogger(classOf[InstanceClient])

  implicit object RequestLogger extends Loggable[HttpRequest] {
    override def asLogMessage(it: HttpRequest): String = s"httpRequest $it"
  }

  def pipeline: Pipeline = {
    sendReceive
  }

  def register(): Future[Unit] = {
    val registration = Registration(
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
    }) map { _ ⇒}
  }
}
