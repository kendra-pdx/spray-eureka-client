package io.github.kender.spray.eureka.example

import scala.concurrent.Future._
import scala.concurrent._
import scala.util.{Random, Success}

import akka.actor.ActorSystem
import spray.client.pipelining._
import spray.routing.{Directives, SimpleRoutingApp}

import io.github.kender.spray.eureka.client.{EurekaConfig, HeartbeatClient, InstanceClient, RestClient}
import org.slf4j.LoggerFactory

object CompositeServiceMain extends App with SimpleRoutingApp with Directives {
  val logger = LoggerFactory.getLogger("io.github.kender.service.composite.CompositeServiceMain")

  implicit val actorSystem = ActorSystem()
  import io.github.kender.spray.eureka.example.CompositeServiceMain.actorSystem.dispatcher

  val eurekaConfig = EurekaConfig(actorSystem)
  val backendRestClient = RestClient(eurekaConfig, "backend")
  val backend = backendRestClient(sendReceive ~> unmarshal[String]) _

  def shim(): Future[String] = {
    for {
      r1 ← backend(Get("/random?length=10"))
      r2 ← backend(Get("/random?length=16"))
    } yield {
      r1 + ":" + r2
    }
  }

  def random(length: Int): Future[String] = successful {
    Random.alphanumeric.take(length).mkString
  }

  startServer("0.0.0.0", 6001, backlog = 500) {
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

  new InstanceClient(eurekaConfig) {
    register() map { instanceId ⇒
      new HeartbeatClient(eurekaConfig) {
        start(() ⇒ Success {/* OK */}, instanceId)
      }
    }
  }
}
