package modules.imagefetcher

import akka.actor.{Actor, Props}
import akka.actor.Actor.Receive

object FetcherActor {
  def props = Props[FetcherActor]
}

class FetcherActor extends Actor {
  override def receive: Receive = {
    case _ => sender() ! "Image Fetcher not yet implemented"
  }
}
