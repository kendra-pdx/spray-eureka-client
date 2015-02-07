package io.github.kender.spray.eureka

import scala.concurrent.Future
import scala.util.Try
import scala.util.control.NonFatal

import spray.http.{HttpResponse, HttpRequest}

package object client {
  case class Instance(id: String)

  type HttpPipeline[In, Out] = HttpRequest ⇒ Future[HttpResponse]
  type HealthCheck = () ⇒ Try[Unit]

  implicit class TryHelpers[T](tried: Try[T]) {
    def onFailure(handler: Throwable ⇒ Unit) = {
      tried recoverWith { case NonFatal(t) ⇒
        handler(t)
        tried
      }
    }
  }

}
