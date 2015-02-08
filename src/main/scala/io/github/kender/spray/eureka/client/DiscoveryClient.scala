package io.github.kender.spray.eureka.client

import scala.concurrent.Future

import akka.actor.ActorSystem
import spray.client.pipelining._
import spray.http.HttpHeaders.RawHeader
import spray.http._

import io.github.kender.spray.eureka.Application

class DiscoveryClient(eurekaConfig: EurekaConfig)(implicit actorSystem: ActorSystem) {
  import actorSystem.dispatcher
  import io.github.kender.spray.eureka.EurekaJsonProtocol._
  import spray.httpx.SprayJsonSupport._

  def pipeline: HttpRequest ⇒ Future[Option[Application]] = {
    addHeader(RawHeader("Accept", "application/json")) ~>
      sendReceive ~>
      unmarshal[Option[Application]]
  }

  def vips(vipAddress: String): Future[Option[Application]] = {
    val uri = s"${eurekaConfig.serverUrl}/v2/vips/$vipAddress"
    pipeline(Get(uri))
  }

  def svips(vipAddress: String): Future[Option[Application]] = {
    val uri = s"${eurekaConfig.serverUrl}/v2/svips/$vipAddress"
    pipeline(Get(uri))
  }
}
