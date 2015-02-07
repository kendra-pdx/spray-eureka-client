package io.github.kender.spray.eureka.client

import scala.concurrent.duration._

import akka.actor.ActorSystem

import com.typesafe.config.{ConfigFactory, Config}

trait EurekaHeartbeatConfig {
  def interval: FiniteDuration
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
}

object EurekaConfig {
  val baseConfigKey = "spray.eureka.client"

  def apply(actorSystem: ActorSystem): EurekaConfig = apply(actorSystem.settings.config.getConfig(baseConfigKey))

  def apply(config: Config = ConfigFactory.load().getConfig(baseConfigKey)) = new EurekaConfig {
    override lazy val serverUrl = {
      var url = config.getString("server.url")
      while (url endsWith "/") url = url.init
      url
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
  }
}
