package io.github.kender.spray.eureka.client

import scala.concurrent.Future._
import scala.concurrent._

import akka.actor.ActorSystem
import spray.http._

import io.github.kender.spray.eureka.InstanceInfo

object RestClient {
  abstract class RestClientException(message: String, cause: Throwable) extends RuntimeException(message, cause)
  class NoHostsForVipException(vip: String, useSecure: Boolean)
    extends RestClientException(s"no instances available for $vip (${if (useSecure) "secure" else "normal"})", null)
}
class RestClient(eurekaConfig: EurekaConfig, restClientId: String)(implicit actorSystem: ActorSystem) {
  import actorSystem.dispatcher
  import io.github.kender.spray.eureka.client.RestClient._

  val restClientConfig = eurekaConfig.restClientConfig(restClientId)

  val discoveryClient = new DiscoveryClient(eurekaConfig)
  val vipLookup = VipLookup(discoveryClient,
    restClientConfig.vipAddress, restClientConfig.useSecure,
    restClientConfig.refreshInterval, restClientConfig.lookupTimeout)

  def findBestInstanceInfo(instanceInfos: Seq[InstanceInfo]): Future[InstanceInfo] = {
    if (instanceInfos.isEmpty) {
      failed(new NoHostsForVipException(restClientConfig.vipAddress, restClientConfig.useSecure))
    } else {
      successful(???)
    }
  }

  def instanceUri(instanceInfo: InstanceInfo): Future[Uri] = Future {
    val uri = Uri().withHost(instanceInfo.hostName)
    if (restClientConfig.useSecure) {
      uri.withScheme("https").withPort(instanceInfo.securePort)
    } else {
      uri.withScheme("http").withPort(instanceInfo.port.getOrElse(0))
    }
  }

  def findUriForVip(): Future[Uri] = for {
    instanceInfos ← vipLookup()
    instanceInfo ← findBestInstanceInfo(instanceInfos)
    instanceUri ← instanceUri(instanceInfo)
  } yield instanceUri

  def lookupVipUri(baseUri: Uri): Future[Uri] = for {
    vipUri ← findUriForVip()
  } yield {
    baseUri.withScheme(vipUri.scheme)
      .withAuthority(vipUri.authority)
      .withPort(vipUri.effectivePort)
  }

  def apply[ResponseEntity](pipeline: HttpRequest ⇒ Future[ResponseEntity])(request: HttpRequest): Future[ResponseEntity] = {
    lookupVipUri(request.uri) flatMap { vipUri ⇒
      pipeline(request.copy(uri = vipUri))
    }
  }
}
