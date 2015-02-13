package io.github.kender.spray.eureka.client

import akka.actor.ActorSystem
import io.github.kender.spray.eureka.AmazonMetaData
import org.json4s.JsonAST.JObject
import org.json4s.jackson.Serialization
import org.json4s.{Formats, NoTypeHints, _}
import spray.client.pipelining._
import spray.http._
import spray.httpx.Json4sJacksonSupport

import scala.concurrent.Future
import scala.concurrent.Future._

class AmazonMetaDataClient(implicit actorSystem: ActorSystem) extends Json4sJacksonSupport {

  import actorSystem.dispatcher

  override implicit val json4sJacksonFormats: Formats = Serialization.formats(NoTypeHints)

  def httpString: HttpRequest => Future[String] = {
    sendReceive ~> unmarshal[String]
  }

  def httpJObject: HttpRequest => Future[JObject] = {
    sendReceive ~> unmarshal[JObject]
  }

  def field(name: String): Future[String] = {
    httpString(Get(s"http://169.254.169.254/latest/meta-data/$name"))
  }

  def dynamic(): Future[JObject] = {
    httpJObject(Get("http://169.254.169.254/latest/dynamic/instance-identity/document"))
  }

  def metaData(): Future[AmazonMetaData] = for {
    metaData <- sequence {
      List("public-ipv4", "public-hostname", "local-hostname") map { key =>
        field(key) map { key -> _ }
      }
    } map { _.toMap }
    dynamic <- dynamic()
  } yield {
    AmazonMetaData(
      (dynamic \ "instanceId").extract[String],
      (dynamic \ "imageId").extract[String],
      (dynamic \ "availabilityZone").extract[String],
      metaData("local-hostname"),
      metaData("public-ipv4"),
      metaData("public-hostname"),
      (dynamic \ "instanceType").extract[String]
    )
  }
}
