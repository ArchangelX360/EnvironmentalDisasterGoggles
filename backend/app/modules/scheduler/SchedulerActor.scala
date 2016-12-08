package modules.scheduler

import akka.actor.{Actor, Props}
import models.Process

import scala.collection.mutable

object SchedulerActor {

  def props = Props[SchedulerActor]

  /**
    * Message definition
    */
  case class StartProcess(query: String)
  case class UpdateProcess(id: String, status: String)
}

class SchedulerActor extends Actor {

  import SchedulerActor._

  val processList = mutable.MutableList.empty[Process]

  override def receive: Receive = {
    case StartProcess(query) => sender ! startProcess(query)
    case "List" => sender ! processList.map(process => process.toString).mkString("\n")
    case _ => ()
  }

  def startProcess(query: String): Process = {
    val process = Process(
      id = String.valueOf(Math.random()),
      query = query,
      status = "started",
      tasks = List.empty)

    processList += process

    process
  }

}
