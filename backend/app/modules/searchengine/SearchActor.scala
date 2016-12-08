package modules.searchengine

import java.security.InvalidParameterException

import akka.actor.{Actor, ActorRef, Props}
import modules.scheduler.SchedulerActor
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.concurrent.duration._

import models.Process

object SearchActor {
  def props(scheduler: ActorRef) = Props(new SearchActor(scheduler))

  case class SearchMessage(message: String)
}

class SearchActor (scheduler: ActorRef) extends Actor {

  import SearchActor._

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
    * @param message Search query send by the web interface
    * @return A confirmation that the message is scheduled for processing
    */
  def processMessage(message: String): Process = {
    val schedulerResponse = (scheduler ? SchedulerActor.StartProcess(message)).mapTo[Process]

    // We need information on the process to go further, therefore we wait for the process creation
    val process = Await.result(schedulerResponse, Duration.Inf)


    process
  }
}
