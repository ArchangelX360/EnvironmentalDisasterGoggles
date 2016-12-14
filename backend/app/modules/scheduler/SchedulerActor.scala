package modules.scheduler

import akka.actor.{Actor, ActorRef, Props}
import models.Query.Queries
import modules.imagefetcher.FetcherActor
import modules.scheduler.SchedulerActor.{CancelProcessing, StartProcessing}
import play.api.libs.ws.WSClient

object SchedulerActor {
  def props(processes: Queries,
            configuration: play.api.Configuration,
            ws: WSClient,
            monitoring: ActorRef): Props =
    Props(new SchedulerActor(processes, configuration, ws, monitoring))

  /**
    * Message definitions
    */
  case class StartProcessing(id: String)
  case class CancelProcessing(id: String)
}

/**
  * This actor manage lifecycle of tasks and queries
  */
class SchedulerActor(processes: Queries, configuration: play.api.Configuration, ws: WSClient, monitoring: ActorRef) extends Actor {

  private val fetcherActor = context.actorOf(FetcherActor.props(ws, configuration.underlying.getString("pfs.servers.imageFetcherUrl"), monitoring))

  override def receive: Receive = {
    case StartProcessing(id) => startProcessing(id)
    case CancelProcessing(id) => cancelProcessing(id)
  }

  def startProcessing(id: String): Unit = {

  }

  def cancelProcessing(id: String): Unit = {
    val index = processes.indexWhere(query => query.id == id)
    processes.remove(index)
  }
}
