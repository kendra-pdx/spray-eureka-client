package io.github.kender.spray.eureka

import scala.concurrent.Future
import scala.util.Try
import scala.util.control.NonFatal

import spray.http.HttpRequest

import org.slf4j.Logger

package object client {
  type HttpPipeline[Out] = HttpRequest ⇒ Future[Out]
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
