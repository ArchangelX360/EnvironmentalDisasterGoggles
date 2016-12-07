package modules.scheduler

import akka.actor.{Actor, Props}
import akka.actor.Actor.Receive

import scala.collection.mutable

object SchedulerActor {

  case class Process(id: String, status: String, name: String)

  def props = Props[SchedulerActor]

  def process = mutable.ListBuffer.empty[Process]

  /**
    * Message definition
    */
  case class Begin(id: String, name: String)
  case class End(id: String)
}

class SchedulerActor extends Actor {

  import SchedulerActor._

  override def receive: Receive = {
    case Begin(id, name) => ()
    case End(id) => ()
    case "List" => sender ! SchedulerActor.process.map(process => process.toString).mkString("\n")
    case _ => ()
  }
}
