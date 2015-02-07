package io.github.kender.spray.eureka

import spray.json.DefaultJsonProtocol

trait EurekaJsonProtocol extends DefaultJsonProtocol {
  implicit lazy val dataCenterInfoFormat = jsonFormat1(DataCenterInfo)
  implicit lazy val leaseInfoFormat = jsonFormat0(LeaseInfo)
  implicit lazy val metaDataFormat = jsonFormat0(MetaData)
  implicit lazy val registrationFormat = jsonFormat14(Registration)
}

object EurekaJsonProtocol extends EurekaJsonProtocol
