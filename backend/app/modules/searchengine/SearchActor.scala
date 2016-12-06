package modules.searchengine

import java.security.InvalidParameterException

import akka.actor.{Actor, Props}
import akka.actor.Actor.Receive

object SearchActor {
  def props = Props[SearchActor]

  case class SearchMessage(message: String)
}

class SearchActor extends Actor {

  import SearchActor._

  /**
    * Handle incoming message send to this actor
    */
  override def receive: Receive = {
    case SearchMessage(message) => sender ! processMessage(message)
    case _ => throw new InvalidParameterException()
  }

  /**
    * Handle search message, send it to spotlight for TAL and contact other actors to schedule the processing
    * @param message Search query send by the web interface
    * @return A confirmation that the message is scheduled for processing
    */
  def processMessage(message: String): String = {
    message + " traited"
  }
}
