package io.github.kender.spray.eureka

case class DataCenterInfo(name: String = "MyOwn")

case class LeaseInfo()

case class MetaData()

case class Application(
  name: String,
  instances: Seq[InstanceInfo])

case class Applications(
  application: Application)

case class InstanceInfo(
  hostName: String,
  app: String,
  ipAddr: String,
  vipAddress: String,
  secureVipAddress: String,
  status: String,
  port: Option[Int],
  securePort: Int,
  homePageUrl: String,
  statusPageUrl: String,
  healthCheckUrl: String,
  dataCenterInfo: DataCenterInfo,
  leaseInfo: Option[LeaseInfo] = None,
  metadata: Option[MetaData] = None) {
  require(port.forall(_ > 0))
  require(securePort > 0)
}

