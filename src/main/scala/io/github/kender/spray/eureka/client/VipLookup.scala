package io.github.kender.spray.eureka.client

import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor._
import akka.event.LoggingReceive
import akka.util.Timeout

import io.github.kender.spray.eureka.InstanceInfo

trait VipLookup {
  def apply(): Future[Seq[InstanceInfo]]
}

object VipLookup {
  def apply(
    discoveryClient: DiscoveryClient,
    vip: String,
    useSecure: Boolean,
    refreshInterval: FiniteDuration,
    lookupTimeout: Timeout)(
    implicit actorSystem: ActorSystem)
  : VipLookup = new VipLookup {
    private val props: Props = Props(new VipLookupActor(discoveryClient, vip, useSecure, refreshInterval))
    val vipLookupActor = actorSystem.actorOf(props)

    import akka.pattern.ask
    import actorSystem.dispatcher

    override def apply() = vipLookupActor.ask(VipLookupActor.GetCache)(lookupTimeout)
      .mapTo[VipLookupActor.GetCacheResponse]
      .map { _.cache }
  }
}

object VipLookupActor {
  sealed trait Protocol
  case class RefreshCache(respondTo: Seq[ActorRef] = Nil) extends Protocol
  case class SetCache(cache: Seq[InstanceInfo], respondTo: Seq[ActorRef])
  case object GetCache extends Protocol
  case class GetCacheResponse(cache: Seq[InstanceInfo]) extends Protocol
}

class VipLookupActor(
  discoveryClient: DiscoveryClient,
  vip: String,
  useSecure: Boolean,
  refreshInterval: FiniteDuration) extends Actor with ActorLogging {
  import VipLookupActor._
  import akka.pattern.pipe
  import context.dispatcher

  var instanceInfos: Seq[InstanceInfo] = Nil
  
  val vips = if (useSecure) {
    discoveryClient.svips _
  } else {
    discoveryClient.vips _
  }

  override def receive = LoggingReceive {
    case RefreshCache(respondTo) ⇒
      vips(vip)
        .map(app ⇒ SetCache(app.fold(Seq.empty[InstanceInfo])(_.instances), respondTo))
        .pipeTo(self)

    case SetCache(newCache, respondTo) ⇒ 
      instanceInfos = newCache
      respondTo map { _ ! GetCacheResponse(instanceInfos)}
      
    case GetCache if instanceInfos.nonEmpty ⇒
      sender ! GetCacheResponse(instanceInfos)

    case GetCache if instanceInfos.isEmpty ⇒
      self ! RefreshCache(sender :: Nil)
  }

  override def preStart() = {
    context.system.scheduler.schedule(0.millis, refreshInterval, self, RefreshCache())
  }
}

