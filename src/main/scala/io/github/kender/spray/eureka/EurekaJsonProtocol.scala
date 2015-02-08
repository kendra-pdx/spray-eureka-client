package io.github.kender.spray.eureka

import scala.collection.JavaConverters._

import spray.json._

trait EurekaJsonProtocol extends DefaultJsonProtocol {
  implicit lazy val dataCenterInfoFormat = jsonFormat1(DataCenterInfo)
  implicit lazy val leaseInfoFormat = jsonFormat0(LeaseInfo)
  implicit lazy val metaDataFormat = jsonFormat0(MetaData)
  implicit lazy val registrationFormat = jsonFormat14(InstanceInfo)

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

    override def read(json: JsValue) = json.asJsObject.getFields("name", "instance") match {
      case JsString(name) :: (instance: JsObject) :: Nil ⇒ Application(name, Seq(instance.convertTo[InstanceInfo]))
      case JsString(name) :: JsArray(instances) :: Nil ⇒ Application(name, instances.map(_.convertTo[InstanceInfo]))
    }
  }
}

object EurekaJsonProtocol extends EurekaJsonProtocol
