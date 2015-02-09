package io.github.kender.spray.eureka.client

import java.util.concurrent.atomic.AtomicLong

sealed trait ChoosingStrategy {
  def apply[R](items: Seq[R]): R
}

object ChoosingStrategy {
  def apply(strategy: RestClientConfig.LoadBalancingStrategy): ChoosingStrategy = strategy match {
    case RestClientConfig.LoadBalancingStrategy.Random ⇒ Random
    case RestClientConfig.LoadBalancingStrategy.RoundRobin ⇒ RoundRobin()
    case RestClientConfig.LoadBalancingStrategy.First ⇒ AlwaysPickFirst
  }
  
  object AlwaysPickFirst extends ChoosingStrategy {
    override def apply[R](items: Seq[R]) = items.head
  }
  
  object Random extends ChoosingStrategy {
    override def apply[R](items: Seq[R]) = {
      assert(items.size > 0)
      items(util.Random.nextInt(items.size))
    }
  }
  
  case class RoundRobin() extends ChoosingStrategy {
    val counter = new AtomicLong()
    override def apply[R](items: Seq[R]) = {
      items((counter.incrementAndGet() % items.size).toInt)
    }
  }
}