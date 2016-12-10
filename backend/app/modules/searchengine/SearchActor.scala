package modules.searchengine

import java.security.InvalidParameterException

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import models.{Query, Task}
import modules.scheduler.MonitoringActor.{StartProcess, StartTask}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.duration._

object SearchActor {
  def props(scheduler: ActorRef) = Props(new SearchActor(scheduler))

  case class SearchMessage(message: String, author: String)
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
    case SearchMessage(message, author) => processMessage(message, author)
    case _ => throw new InvalidParameterException()
  }

  /**
    * Handle search message, send it to spotlight for TAL and contact other actors to schedule the processing
 *
    * @param message Search query send by the web interface
    * @return A confirmation that the message is scheduled for processing
    */
  def processMessage(message: String, author: String) = {
    val schedulerResponse = (scheduler ? StartProcess(message, author)).mapTo[Query]

    val taskResponse = schedulerResponse flatMap (query =>
      (scheduler ? StartTask(query.id, "Search task")).mapTo[Task]
    )

    sender ! "Started"

  }
}
