package io.github.kender.spray.eureka.client

import scala.annotation.tailrec
import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.util.Timeout

import com.typesafe.config.{ConfigFactory, Config}

trait EurekaHeartbeatConfig {
  def interval: FiniteDuration
}

object RestClientConfig {
  sealed trait LoadBalancingStrategy
  object LoadBalancingStrategy {
    case object Random extends LoadBalancingStrategy
    case object RoundRobin extends LoadBalancingStrategy
    case object First extends LoadBalancingStrategy
  }

  sealed trait LoadBalancingLocality
  object LoadBalancingLocality {
    case object AvailabilityZone extends LoadBalancingLocality
    case object Region extends LoadBalancingLocality
    case object AnyHost extends LoadBalancingLocality
  }
}

trait RestClientConfig {
  import RestClientConfig._
  def vipAddress: String
  def refreshInterval: FiniteDuration
  def useSecure: Boolean
  def loadBalancingLocalityPriority: Seq[LoadBalancingLocality]
  def loadBalancingStrategy: LoadBalancingStrategy
  def lookupTimeout: Timeout
}

trait EurekaInstanceConfig {
  def hostName: String
  def appId: String
  def ipAddress: String
  def vipAddress: String
  def secureVipAddress: String
  def port: Int
  def securePort: Int
  def homePageUrl: String
  def statusPageUrl: String
  def healthCheckUrl: String
}

trait EurekaConfig {
  def serverUrl: String
  def heartbeat: EurekaHeartbeatConfig
  def instance: EurekaInstanceConfig
  def restClientConfig(clientName: String): RestClientConfig
}

object EurekaConfig {
  val baseConfigKey = "spray.eureka.client"

  def apply(actorSystem: ActorSystem): EurekaConfig = apply(actorSystem.settings.config.getConfig(baseConfigKey))

  def apply(config: Config = ConfigFactory.load().getConfig(baseConfigKey)) = new EurekaConfig {
    override lazy val serverUrl = {
      @tailrec def strip(url: String): String = if (!(url endsWith "/")) url else strip(url.init)
      strip(config.getString("server.url"))
    }

    override lazy val heartbeat: EurekaHeartbeatConfig = new EurekaHeartbeatConfig {
      override lazy val interval = config.getDuration("heartbeat.interval", MILLISECONDS).millis
    }

    override lazy val instance: EurekaInstanceConfig = new EurekaInstanceConfig {
      private lazy val instanceConfig = config.getConfig("instance")

      override lazy val hostName = instanceConfig.getString("hostName")
      override lazy val appId = instanceConfig.getString("appId")
      override lazy val vipAddress = instanceConfig.getString("vipAddress")
      override lazy val ipAddress = instanceConfig.getString("ipAddress")
      override lazy val port = instanceConfig.getInt("port")
      override lazy val securePort = instanceConfig.getInt("securePort")
      override lazy val statusPageUrl = instanceConfig.getString("statusPageUrl")
      override lazy val secureVipAddress = instanceConfig.getString("secureVipAddress")
      override lazy val homePageUrl = instanceConfig.getString("homePageUrl")
      override lazy val healthCheckUrl = instanceConfig.getString("healthCheckUrl")
    }

    override def restClientConfig(clientName: String) = {
      val restClientConfig = config.getConfig(s"rest.$clientName").withFallback(config.getConfig(s"rest.defaults"))

      new RestClientConfig {
        import RestClientConfig._
        import scala.collection.JavaConverters._
        override lazy val vipAddress = restClientConfig.getString("vipAddress")
        override lazy val refreshInterval = restClientConfig.getDuration("refreshInterval", MILLISECONDS).millis
        override lazy val useSecure = restClientConfig.getBoolean("useSecure")

        override lazy val loadBalancingLocalityPriority = {
          restClientConfig.getStringList("loadBalancingLocalityPriority").asScala.toSeq.map {
            case "az" ⇒ LoadBalancingLocality.AvailabilityZone
            case "region" ⇒ LoadBalancingLocality.Region
            case "any" ⇒ LoadBalancingLocality.AnyHost
            case unknown ⇒ sys.error(s"unsupported load balancing locality: $unknown")
          }
        }

        override lazy val loadBalancingStrategy = restClientConfig.getString("loadBalancingStrategy").toLowerCase match {
          case "random" ⇒ LoadBalancingStrategy.Random
          case "round-robin" ⇒ LoadBalancingStrategy.RoundRobin
          case "first" ⇒ LoadBalancingStrategy.First
          case unknown ⇒ sys.error(s"unsupported load balancing strategy: $unknown")
        }

        override lazy val lookupTimeout = new Timeout(restClientConfig.getDuration("lookupTimeout", MILLISECONDS), MILLISECONDS)
      }
    }
  }
}
