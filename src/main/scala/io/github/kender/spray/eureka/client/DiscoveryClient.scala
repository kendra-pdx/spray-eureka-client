package io.github.kender.spray.eureka.client

import scala.concurrent.Future

import akka.actor.ActorSystem
import spray.client.pipelining._
import spray.http.HttpHeaders.RawHeader
import spray.http._

import io.github.kender.spray.eureka.{Applications, Application}

object DiscoveryClient {
  import io.github.kender.spray.eureka.EurekaJsonProtocol._

  case class VipLookupResponse(applications: Applications)
  implicit val vipLookupResponseFormat = jsonFormat1(VipLookupResponse)
}

/**
 * A client for performing service discovery operations on Eureka
 * @param eurekaConfig EurekaConfig
 * @param actorSystem ActorSystem
 */
class DiscoveryClient(eurekaConfig: EurekaConfig)(implicit actorSystem: ActorSystem) {
  import actorSystem.dispatcher
  import spray.httpx.SprayJsonSupport._
  import DiscoveryClient._

  private def http: HttpRequest ⇒ Future[Option[VipLookupResponse]] = {
    addHeader(RawHeader("Accept", "application/json")) ~>
      sendReceive ~>
      unmarshal[Option[VipLookupResponse]]
  }

  /**
   * Lookup an application by vip
   * @param vipAddress the target vip
   * @return A future of optional application. None with not found.
   */
  def vips(vipAddress: String): Future[Option[Application]] = {
    val uri = s"${eurekaConfig.serverUrl}/v2/vips/$vipAddress"
    http(Get(uri)).map(_.map(_.applications.application))
  }

  /**
   * Lookup an application by secure vip
   * @param vipAddress the target secure vip
   * @return A future of optional application. None with not found.
   */
  def svips(vipAddress: String): Future[Option[Application]] = {
    val uri = s"${eurekaConfig.serverUrl}/v2/svips/$vipAddress"
    http(Get(uri)).map(_.map(_.applications.application))
  }
}
