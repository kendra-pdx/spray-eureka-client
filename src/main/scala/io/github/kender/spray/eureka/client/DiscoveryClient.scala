package io.github.kender.spray.eureka.client

import scala.concurrent.Future

import akka.actor.ActorSystem
import spray.client.pipelining._
import spray.http.HttpHeaders.RawHeader
import spray.http._

import io.github.kender.spray.eureka.{Applications, Application}

object DiscoveryClient {
  import io.github.kender.spray.eureka.EurekaJsonProtocol._
  import spray.json._

  case class VipLookupResponse(applications: Applications)
  implicit val vipLookupResponseFormat = jsonFormat1(VipLookupResponse)
}
class DiscoveryClient(eurekaConfig: EurekaConfig)(implicit actorSystem: ActorSystem) {
  import actorSystem.dispatcher
  import spray.httpx.SprayJsonSupport._
  import DiscoveryClient._

  def http: HttpRequest ⇒ Future[Option[VipLookupResponse]] = {
    addHeader(RawHeader("Accept", "application/json")) ~>
      sendReceive ~>
      unmarshal[Option[VipLookupResponse]]
  }

  def vips(vipAddress: String): Future[Option[Application]] = {
    val uri = s"${eurekaConfig.serverUrl}/v2/vips/$vipAddress"
    http(Get(uri)).map(_.map(_.applications.application))
  }

  def svips(vipAddress: String): Future[Option[Application]] = {
    val uri = s"${eurekaConfig.serverUrl}/v2/svips/$vipAddress"
    http(Get(uri)).map(_.map(_.applications.application))
  }
}
