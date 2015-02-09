package io.github.kender.spray.eureka

import spray.json._

trait EurekaJsonProtocol extends DefaultJsonProtocol {
  implicit lazy val dataCenterInfoFormat = jsonFormat1(DataCenterInfo)
  implicit lazy val leaseInfoFormat = jsonFormat0(LeaseInfo)
  implicit lazy val metaDataFormat = jsonFormat0(MetaData)
  
  implicit lazy val instanceInfoFormat =  new RootJsonFormat[InstanceInfo] {
    lazy val format = jsonFormat14(InstanceInfo)
    override def write(obj: InstanceInfo) = format.write(obj)
    override def read(json: JsValue) = {
      val jsObject = json.asJsObject
      def field[F : JsonReader](key: String) = jsObject.fields.get(key).map(_.convertTo[F])
      def str(key: String) = field[String](key).get
      def ostr(key: String) = field[String](key)

      def port(key: String) = jsObject.fields.get(key) map {
        case portJsObject: JsObject ⇒ portJsObject.fields("$").convertTo[String].toInt
        case JsNumber(value) ⇒ value.toInt
        case _ ⇒ sys.error("port must be a number or object")
      }

      InstanceInfo(
        hostName = str("hostName"),
        app = str("app"),
        ipAddr = str("ipAddr"),
        vipAddress = str("vipAddress"),
        secureVipAddress = ostr("secureVipAddress") getOrElse "",
        status = str("status"),
        port = port("port"),
        securePort = port("securePort").get,
        homePageUrl = str("homePageUrl"),
        statusPageUrl = str("statusPageUrl"),
        healthCheckUrl = str("healthCheckUrl"),
        dataCenterInfo = field[DataCenterInfo]("dataCenterInfo").get
      )
    }
  }

  implicit lazy val  applicationFormat = new RootJsonFormat[Application] {
    override def write(application: Application) = {
      var fields: Seq[JsField] = Seq(
        "name" → application.name.toJson
      )
      if (application.instances.size == 1) {
        fields :+= ("instance", application.instances.head.toJson)
      } else if (application.instances.size > 1) {
        fields :+= ("instance", application.instances.toJson)
      }
      JsObject(fields: _*)
    }

    override def read(json: JsValue) = {
      val jsObject = json.asJsObject
      List("name", "instance").map(jsObject.fields.get) match {
        case Some(JsString(name)) :: None :: Nil ⇒ Application(name, Nil)
        case Some(JsString(name)) :: Some((instance: JsObject)) :: Nil ⇒ Application(name, Vector(instance.convertTo[InstanceInfo]))
        case Some(JsString(name)) :: Some(JsArray(instances)) :: Nil ⇒ Application(name, instances.map(_.convertTo[InstanceInfo]))
        case _ ⇒
          sys.error(s"invalid application: $jsObject")
      }
    }
  }

  implicit lazy val applicationsFormat = jsonFormat1(Applications)

}

object EurekaJsonProtocol extends EurekaJsonProtocol
