package modules.scheduler

import akka.actor.{Actor, Props}
import models.{Query, Task}

import scala.collection.mutable

object MonitoringActor {

  /**
    * Create a new instance of monitoring actor
    */
  def props = Props[MonitoringActor]

  /**
    * Message definition
    */
  case class StartProcess(query: String, author: String)
  case class UpdateProcess(id: String, status: String)
  case class ListProcess(status: Option[String] = None)

  case class StartTask(processId: String, name: String)
  case class UpdateTask(processId: String, taskId: String, status: Option[String] = None, progress: Option[Int] = None)
}

class MonitoringActor extends Actor {

  /**
    * Implicit import from companion object
    */
  import MonitoringActor._

  /**
    * List of all the process run by the server (including finished ones)
    */
  val processList = mutable.MutableList.empty[Query]

  /**
    * Message handling
    */
  override def receive: Receive = {
    case StartProcess(query, author) => sender ! startProcess(query, author)
    case ListProcess(status) => sender ! listProcess(status)
    case _ => ()
  }

  /**
    * Create monitoring for a new process/query
    */
  def startProcess(query: String, author: String): Query = {
    val process = Query(
      id = String.valueOf(Math.random()), // Generate a random id
      name = query,
      author = author,
      status = "started",
      tasks = List.empty)

    processList += process

    process
  }

  /**
    * List all the process which subscribed to the monitoring service
    * @param status Filter on the status field
    */
  def listProcess(status: Option[String]): Seq[Query] = {
    if (status.isDefined) processList.filter(process => process.status == status.get)
    else processList
  }

}
