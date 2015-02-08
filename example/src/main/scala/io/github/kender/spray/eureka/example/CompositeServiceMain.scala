package io.github.kender.spray.eureka.example

import scala.concurrent.Future._
import scala.concurrent._
import scala.util.{Random, Success}

import akka.actor.ActorSystem
import spray.client.pipelining._
import spray.http._
import spray.routing.{Directives, SimpleRoutingApp}

import io.github.kender.spray.eureka.client.{HeartbeatClient, InstanceClient, EurekaConfig}
import org.slf4j.LoggerFactory

object CompositeServiceMain extends App with SimpleRoutingApp with Directives {
  val logger = LoggerFactory.getLogger("io.github.kender.service.composite.CompositeServiceMain")

  implicit val actorSystem = ActorSystem()
  import actorSystem.dispatcher


  def shim(): Future[String] = {
    val http: HttpRequest ⇒ Future[String] = {
      sendReceive ~> unmarshal[String]
    }

    for {
      r1 ← http(Get("http://[::1]:6001/random?length=10"))
      r2 ← http(Get("http://[::1]:6001/random?length=16"))
    } yield {
      r1 + ":" + r2
    }

  }

  def random(length: Int): Future[String] = successful {
    Random.alphanumeric.take(length).mkString
  }

  startServer("::1", 6001, backlog = 500) {
    (get & path("random") & parameter('length.?)) { length ⇒
      onSuccess(random(length.map(_.toInt).getOrElse(32))) { random ⇒
        complete(random)
      }
    } ~
    (get & pathEndOrSingleSlash) {
      onSuccess(shim()) { value ⇒
        complete(value)
      }
    }
  }

  val eurekaConfig = EurekaConfig(actorSystem)
  new InstanceClient(eurekaConfig) {
    register() map { instanceId ⇒
      new HeartbeatClient(eurekaConfig) {
        start(() ⇒ Success {/* OK */}, instanceId)
      }
    }
  }
}
