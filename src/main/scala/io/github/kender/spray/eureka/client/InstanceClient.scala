
package io.github.kender.spray.eureka.client

import org.json4s.jackson.Serialization
import org.json4s.{NoTypeHints, DefaultFormats, Formats}
import spray.httpx.Json4sJacksonSupport

import scala.concurrent._

import akka.actor.ActorSystem
import spray.client.pipelining._
import spray.http._

import io.github.kender.spray.eureka.{Port, Registration, DataCenterInfo, InstanceInfo}
import org.slf4j.LoggerFactory

/**
 * A client for managing a service instance with Eureka
 * @param config EurekaConfig
 * @param actorSystem ActorSystem
 */
class InstanceClient(config: EurekaConfig)(implicit actorSystem: ActorSystem) extends Json4sJacksonSupport {
  val instanceUrl = s"${config.serverUrl}/v2/apps/${config.instance.appId}"

  import actorSystem.dispatcher
  import io.github.kender.spray.eureka.client.Loggable._

  override implicit def json4sJacksonFormats: Formats =
    EurekaSerialization.Implicits.eurekaFormats

  type InstanceId = String

  val logger = LoggerFactory.getLogger(classOf[InstanceClient])

  private implicit object RequestLogger extends Loggable[HttpRequest] {
    override def asLogMessage(it: HttpRequest): String = s"httpRequest $it"
  }

  private def pipeline: HttpRequest ⇒ Future[HttpResponse] = {
    sendReceive
  }

  /**
   * de-register the instance from Eureka
   * @return A future which completes when after the call is complete.
   */
  def deRegister(): Future[Unit] = {
    pipeline(Delete(instanceUrl)).map(_ ⇒ Unit)
  }

  /**
   * register the instance with Eureka
   * @return A future containing the instance id which completes when after the call is complete.
   */
  def register(status: String = "UP", dataCenterInfo: DataCenterInfo = DataCenterInfo.myOwn): Future[InstanceId] = {
    logger.info("registering instance: {}", config.instance.appId)
    val instance = InstanceInfo(
      config.instance.hostName,
      config.instance.appId,
      config.instance.ipAddress,
      config.instance.vipAddress,
      config.instance.secureVipAddress,
      status,
      Some(Port(config.instance.port.toString)),
      Port(config.instance.securePort.toString),
      config.instance.homePageUrl,
      config.instance.statusPageUrl,
      config.instance.healthCheckUrl,
      dataCenterInfo
    )

    pipeline(debugIt(logger) {
      Post(
        instanceUrl,
        Registration(instance))
    }) map { _ ⇒ config.instance.hostName }
  }
}
