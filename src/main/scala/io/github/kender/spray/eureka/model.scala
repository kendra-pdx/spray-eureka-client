package io.github.kender.spray.eureka

import scala.util.control.NonFatal

case class DataCenterInfo(name: String = "MyOwn")

case class LeaseInfo()

case class MetaData()

case class Application(
  name: String,
  instances: Seq[InstanceInfo])

case class Applications(
  application: Application)

case class Registration(
  instance: InstanceInfo
  )

case class Port(
  // thanks, jaxb
  `$`: String)

object Port {
  def apply(port: Int): Port = Port(port.toString)
  def unapply(p: String): Option[Int] = try {
    Some(p.toInt)
  } catch { case NonFatal(_) =>
    None
  }
}

case class InstanceInfo(
  hostName: String,
  app: String,
  ipAddr: String,
  vipAddress: String,
  secureVipAddress: String,
  status: String,
  port: Option[Port],
  securePort: Port,
  homePageUrl: String,
  statusPageUrl: String,
  healthCheckUrl: String,
  dataCenterInfo: DataCenterInfo,
  leaseInfo: Option[LeaseInfo] = None,
  metadata: Option[MetaData] = None) {

  val portNumber = port map { _.`$`.toInt }
  val securePortNumber = securePort.`$`.toInt

  require(portNumber.forall(_ > 0))
  require(securePortNumber > 0)
}

