package io.github.kender.spray.eureka.client

import io.github.kender.spray.eureka.{Application, InstanceInfo}
import org.json4s.JsonAST._
import org.json4s.jackson.Serialization
import org.json4s.{CustomSerializer, NoTypeHints}

object EurekaSerialization {

  class ApplicationSerializer extends CustomSerializer[Application](implicit format => ( {
    case JObject(JField("name", JString(name)) :: Nil) =>
      Application(name, Nil)
    case JObject(JField("name", JString(name)) :: JField("instance", instance: JObject) :: Nil) =>
      Application(name, List(instance.extract[InstanceInfo]))
    case JObject(JField("name", JString(name)) :: JField("instance", JArray(instances)) :: Nil) =>
      Application(name, instances.map(_.extract[InstanceInfo]))

  }, {
    case application: Application =>
      JObject(
        "name" -> JString(application.name)
      )
  }))


  object Implicits {

    implicit val eurekaFormats =
      Serialization.formats(NoTypeHints) +
      new EurekaSerialization.ApplicationSerializer
  }

}