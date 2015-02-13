package io.github.kender.spray.eureka.client

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.control.NonFatal

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
    private val props = Props(new VipLookupActor(discoveryClient, vip, useSecure, refreshInterval))

    val vipLookupActor = actorSystem.actorOf(props)

    import akka.pattern.ask
    import actorSystem.dispatcher

    override def apply() = vipLookupActor.ask(VipLookupActor.GetInstances)(lookupTimeout)
      .mapTo[VipLookupActor.GetInstancesResponse]
      .map { _.cache }
  }
}

object VipLookupActor {
  sealed trait Protocol
  case class RefreshInstances(respondTo: Seq[ActorRef] = Nil) extends Protocol
  case class SetInstances(instances: Seq[InstanceInfo], respondTo: Seq[ActorRef])
  case object GetInstances extends Protocol
  case class GetInstancesResponse(cache: Seq[InstanceInfo]) extends Protocol
}

class VipLookupActor(
  discoveryClient: DiscoveryClient,
  vip: String,
  useSecure: Boolean,
  refreshInterval: FiniteDuration) extends Actor with ActorLogging {
  import VipLookupActor._
  import akka.pattern.pipe
  import context.dispatcher

  var instances: Seq[InstanceInfo] = Nil
  
  val discover = if (useSecure) {
    discoveryClient.svips _
  } else {
    discoveryClient.vips _
  }

  override def receive = LoggingReceive {
    case RefreshInstances(respondTo) ⇒
      log.debug("refreshing instances for {}", vip)
      discover(vip)
        .map(app ⇒ SetInstances(app.instances, respondTo))
        .recover { case NonFatal(t) ⇒
          log.error(t, t.getMessage)
          SetInstances(Nil, respondTo)
        }
        .pipeTo(self)

    case SetInstances(newInstances, respondTo) ⇒
      log.debug("updating known instances for {}", vip)
      instances = newInstances
      respondTo map { _ ! GetInstancesResponse(instances)}

    case GetInstances if instances.isEmpty ⇒
      self ! RefreshInstances(sender :: Nil)

    case GetInstances ⇒
      sender ! GetInstancesResponse(instances)
  }

  override def preStart() = {
    context.system.scheduler.schedule(0.millis, refreshInterval, self, RefreshInstances())
  }
}

