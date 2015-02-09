package io.github.kender.spray.eureka.client

import scala.concurrent.Future._
import scala.concurrent._

import akka.actor.ActorSystem
import spray.http._

import io.github.kender.spray.eureka.InstanceInfo

trait RestClient {
  def apply[ResponseEntity](pipeline: HttpRequest ⇒ Future[ResponseEntity])(request: HttpRequest): Future[ResponseEntity]
}

object RestClient {
  abstract class RestClientException(message: String, cause: Throwable) extends RuntimeException(message, cause)
  class NoHostsForVipException(vip: String, useSecure: Boolean)
    extends RestClientException(s"no instances available for $vip (${if (useSecure) "secure" else "normal"})", null)

  def apply(eurekaConfig: EurekaConfig, restClientId: String)(implicit actorSystem: ActorSystem): RestClient = {
    new EurekaRestClient(eurekaConfig, restClientId)
  }
}

class EurekaRestClient(eurekaConfig: EurekaConfig, restClientId: String)(implicit actorSystem: ActorSystem) extends RestClient {
  import actorSystem.dispatcher
  import io.github.kender.spray.eureka.client.RestClient._

  val restClientConfig = eurekaConfig.restClientConfig(restClientId)

  val vipLookup = VipLookup(new DiscoveryClient(eurekaConfig),
    restClientConfig.vipAddress, restClientConfig.useSecure,
    restClientConfig.refreshInterval, restClientConfig.lookupTimeout)
  
  val choiceStrategy = ChoosingStrategy(restClientConfig.loadBalancingStrategy)
  
  def findBestInstance(instances: Seq[InstanceInfo]): Future[InstanceInfo] = {
    if (instances.isEmpty) {
      failed(new NoHostsForVipException(restClientConfig.vipAddress, restClientConfig.useSecure))
    } else {
      //todo: filter by locality
      val local = instances
      successful(choiceStrategy(local))
    }
  }

  def uriOf(instanceInfo: InstanceInfo): Future[Uri] = Future {
    val uri = Uri().withHost(instanceInfo.hostName)
    if (restClientConfig.useSecure) {
      uri.withScheme("https").withPort(instanceInfo.securePort)
    } else {
      uri.withScheme("http").withPort(instanceInfo.port.getOrElse(0))
    }
  }

  def findUriForVip(): Future[Uri] = for {
    instances ← vipLookup()
    instance ← findBestInstance(instances)
    instanceUri ← uriOf(instance)
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
