package modules.searchengine

import java.security.InvalidParameterException

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import models.Query
import modules.scheduler.MonitoringActor

import scala.concurrent.Await
import scala.concurrent.duration.{Duration, _}

object SearchActor {
  def props(scheduler: ActorRef) = Props(new SearchActor(scheduler))

  case class SearchMessage(message: String)
}

class SearchActor (scheduler: ActorRef) extends Actor {

  /**
    * Import implicit definition from companion object
    */
  import SearchActor._

  /**
    * after this delay the server send a timeout, however the process is still running
    */
  implicit val timeout: Timeout = 5.seconds

  /**
    * Handle incoming message send to this actor
    */
  override def receive: Receive = {
    case SearchMessage(message) => sender ! processMessage(message)
    case _ => throw new InvalidParameterException()
  }

  /**
    * Handle search message, send it to spotlight for TAL and contact other actors to schedule the processing
 *
    * @param message Search query send by the web interface
    * @return A confirmation that the message is scheduled for processing
    */
  def processMessage(message: String): Query = {
    val schedulerResponse = (scheduler ? MonitoringActor.StartProcess(message)).mapTo[Query]

    // We need information on the process to go further, therefore we wait for the process to be initialised
    val process = Await.result(schedulerResponse, Duration.Inf)

    process
  }
}
