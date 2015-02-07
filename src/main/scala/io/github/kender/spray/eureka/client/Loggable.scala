package io.github.kender.spray.eureka.client

import org.slf4j.Logger

trait Loggable[It] {
  def asLogMessage(it: It): String
}

object Loggable {
  def debugIt[It : Loggable](logger: Logger)(it: It): It = {
    if (logger.isDebugEnabled) {
      logger.debug(implicitly[Loggable[It]].asLogMessage(it))
    }
    it
  }
}
