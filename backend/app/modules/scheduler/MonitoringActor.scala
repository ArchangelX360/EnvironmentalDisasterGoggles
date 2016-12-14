package modules.scheduler

import akka.actor.{Actor, Props}
import models.Query.Queries
import models.{Query, Task}
import modules.searchengine.SearchActor.SearchDetails

import scala.collection.mutable

object MonitoringActor {

  /**
    * Create a new instance of monitoring actor
    */
  def props(processList: Queries) = Props(new MonitoringActor(processList))

  /**
    * Message definition
    */
  case class StartProcess(query: String, author: String,  details: Option[SearchDetails])
  case class UpdateProcess(id: String, status: String)
  case class ListProcess(status: Option[String] = None)

  case class StartTask(processId: String, name: String)
  case class UpdateTask(processId: String, taskId: String, status: Option[String] = None, progress: Option[Int] = None)
}

class MonitoringActor(processList: Queries) extends Actor {

  /**
    * Implicit import from companion object
    */
  import MonitoringActor._

  /**
    * Message handling
    */
  override def receive: Receive = {
    case StartProcess(query, author, details) => sender ! startProcess(query, author, details)
    case ListProcess(status) => sender ! listProcess(status)
    case task: StartTask => startTask(task)
    case _ => ()
  }

  /**
    * Create monitoring for a new process/query
    */
  def startProcess(query: String, author: String, details: Option[SearchDetails]): Query = {
    val process = Query(
      id = String.valueOf(Math.random()),
      details = details,
      name = query,
      author = author,
      status = "started",
      tasks = mutable.ListBuffer.empty)

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

  /**
    * Create a new task and match it with a process, notify the sender with the new task instance
    * @param newTask Information about the new Task, including name and process id
    */
  def startTask(newTask: StartTask): Unit = {
    val process = processList.find(process => process.id == newTask.processId)
    val task = Task(
      id = String.valueOf(Math.random()),
      name = newTask.name,
      status = "Started",
      progress = 0,
      metadata = Map.empty
    )

    if (process.isDefined) {
      process.get.tasks.+=(task)
    }

    sender ! task
  }

}
