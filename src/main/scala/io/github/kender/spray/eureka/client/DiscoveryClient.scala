package io.github.kender.spray.eureka.client

import akka.actor.ActorSystem
import io.github.kender.spray.eureka.{Application, Applications}
import org.json4s.Formats
import org.slf4j.LoggerFactory
import spray.client.pipelining._
import spray.http.HttpHeaders.RawHeader
import spray.http._
import spray.httpx.Json4sJacksonSupport

import scala.concurrent.Future

object DiscoveryClient {
  case class VipLookupResponse(applications: Applications)
}

/**
 * A client for performing service discovery operations on Eureka
 * @param eurekaConfig EurekaConfig
 * @param actorSystem ActorSystem
 */
class DiscoveryClient(eurekaConfig: EurekaConfig)(implicit actorSystem: ActorSystem) extends Json4sJacksonSupport {
  import actorSystem.dispatcher
  import io.github.kender.spray.eureka.client.DiscoveryClient._

  val logger = LoggerFactory.getLogger(classOf[DiscoveryClient])

  override implicit def json4sJacksonFormats: Formats = EurekaSerialization.Implicits.eurekaFormats

  private def http: HttpRequest ⇒ Future[VipLookupResponse] = {
    addHeader(RawHeader("Accept", "application/json")) ~>
      sendReceive ~>
      logResponse(r => logger.debug(s"response: {}", r)) ~>
      unmarshal[VipLookupResponse] ~>
      logValue[VipLookupResponse]((r: VipLookupResponse) => logger.debug(s"unmarshaled: {}", r))
  }

  /**
   * Lookup an application by vip
   * @param vipAddress the target vip
   * @return A future of optional application. None with not found.
   */
  def vips(vipAddress: String): Future[Application] = {
    val uri = s"${eurekaConfig.serverUrl}/v2/vips/$vipAddress"
    http(Get(uri)).map(_.applications.application)
  }

  /**
   * Lookup an application by secure vip
   * @param vipAddress the target secure vip
   * @return A future of optional application. None with not found.
   */
  def svips(vipAddress: String): Future[Application] = {
    val uri = s"${eurekaConfig.serverUrl}/v2/svips/$vipAddress"
    http(Get(uri)).map(_.applications.application)
  }
}
