package modules.scheduler

import akka.actor.{Actor, Props}
import models.Query.Queries
import models.{Query, Task}
import modules.searchengine.SearchActor.SearchDetails

import scala.collection.mutable
import scala.util.Random

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
  case class GetProcess(id: String)

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
    case GetProcess(id) => getProcess(id)
    case task: StartTask => startTask(task)
    case task: UpdateTask => updateTask(task)
    case _ => ()
  }

  /**
    * Create monitoring for a new process/query
    */
  def startProcess(query: String, author: String, details: Option[SearchDetails]): Query = {
    val process = Query(
      id = String.valueOf(Random.nextInt()),
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

  /**
    * Return information about a query from its id
    */
  def getProcess(id: String): Unit =
    processList.find(query => query.id == id).foreach(query => sender ! query)

  /**
    * Update task information using optional arguments of the message
    * (Metadata are ignored)
    */
  def updateTask(taskInfo: UpdateTask): Unit = {
    val process = processList
      .find(q => q.id == taskInfo.processId)

    val taskIndex = process.map(q => q.tasks.indexWhere(t => t.id == taskInfo.taskId))

    if (process.isDefined && taskIndex.isDefined) {
      val task = process.get.tasks(taskIndex.get)
      val newTask = Task(task.id, task.name, taskInfo.status.getOrElse(task.status), taskInfo.progress.getOrElse(task.progress), task.metadata)
      process.get.tasks.update(taskIndex.get, newTask)
    }

  }

}
