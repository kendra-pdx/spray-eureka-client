package io.github.kender.spray.eureka.example

import spray.http.HttpRequest

import scala.concurrent.Future._
import scala.concurrent._
import scala.util.{Random, Success}

import akka.actor.ActorSystem
import spray.client.pipelining._
import spray.routing.{Directives, SimpleRoutingApp}

import io.github.kender.spray.eureka.client.{EurekaConfig, HeartbeatClient, InstanceClient, RestClient}
import org.slf4j.LoggerFactory

object CompositeServiceMain extends App with SimpleRoutingApp with Directives {
  implicit val actorSystem = ActorSystem()
  import actorSystem.dispatcher

  val logger = LoggerFactory.getLogger(classOf[CompositeServiceMain])

  // constructs a eureka config by extracting the configuration from the actorSystem
  val eurekaConfig = EurekaConfig(actorSystem)


  // register the instance with Eureka.
  new InstanceClient(eurekaConfig) {
    // when registration is complete, begin sending heartbeats.
    register() map { instanceId ⇒
      new HeartbeatClient(eurekaConfig) {
        // the heartbeat callback always succeeds in this case.
        start(() ⇒ Success {/* OK */}, instanceId)
      }
    }
  }

  // create a new RestClient from the eurekaConfig for the "backend" client configuration.
  val backendRestClient = RestClient(eurekaConfig, "backend")

  // create a 'backend' http function by applying the a basic spray pipeline which makes the call
  // and unmarshals the response into a string
  val backend: (HttpRequest) => Future[String] = {
    backendRestClient(sendReceive ~> unmarshal[String])
  }

  // this function implements the "GET /" route
  def shim(): Future[String] = {
    // these are independent, so they can be declared head of the for-comprehension
    val firstRequest  = backend(Get("/random?length=10"))
    val secondRequest = backend(Get("/random?length=16"))

    // when both requests yield a response, combine them into a single value
    for {
      r1 ← firstRequest
      r2 ← secondRequest
    } yield {
      r1 + ":" + r2
    }
  }

  // this function implements the "GET /random?length" route
  def random(length: Int): Future[String] = successful {
    Random.alphanumeric.take(length).mkString
  }

  // spray routing server and routes
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
}

class CompositeServiceMain