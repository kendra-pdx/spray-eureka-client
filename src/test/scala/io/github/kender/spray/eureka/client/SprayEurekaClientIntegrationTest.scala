package io.github.kender.spray.eureka.client

import scala.concurrent._
import scala.concurrent.duration._
import scala.util.Success

import akka.actor.ActorSystem

import org.scalatest.FlatSpec

class SprayEurekaClientIntegrationTest extends FlatSpec{
  def withActorSystem(testCode: ActorSystem ⇒ Any): Any = {
    val actorSystem = ActorSystem()
    try {
      testCode(actorSystem)
    } finally {
      actorSystem.shutdown()
    }
  }

  def waitForResult[Result](future: Awaitable[Result]): Result = {
    Await.result(future, 10.seconds)
  }

  "spray eureka client" should "be able to register" taggedAs IntegrationTest in withActorSystem { implicit actorSystem ⇒
    lazy val eurekaConfig = EurekaConfig(actorSystem)

    val instanceClient = new InstanceClient(eurekaConfig)
    val instanceId = waitForResult(instanceClient.register())
    println(instanceId)

    val heartbeatClient = new HeartbeatClient(eurekaConfig)
    heartbeatClient.start(() ⇒ Success({}), eurekaConfig.instance.hostName)

    Thread.sleep(2.minutes.toMillis)
  }

}
