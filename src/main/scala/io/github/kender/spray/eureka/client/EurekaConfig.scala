package io.github.kender.spray.eureka.client

import scala.concurrent.duration._

import com.typesafe.config.{ConfigFactory, Config}

trait EurekaHeartbeatConfig {
  def interval: FiniteDuration
}

trait EurekaConfig {
  def serverUrl: String
  def heartbeat: EurekaHeartbeatConfig
}

object EurekaConfig {
  val baseConfigKey = "spray.eureka.client"

  def apply(config: Config = ConfigFactory.load().getConfig(baseConfigKey)) = new EurekaConfig {

    override def serverUrl: String = config.getString("server.url")

    override def heartbeat: EurekaHeartbeatConfig = new EurekaHeartbeatConfig {
      override def interval: FiniteDuration = config.getDuration("heartbeat.interval", MILLISECONDS).millis
    }
  }
}
