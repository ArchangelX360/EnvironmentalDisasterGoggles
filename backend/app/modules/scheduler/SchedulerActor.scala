package modules.scheduler

import akka.actor.{Actor, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import models.Query
import models.Query.Queries
import modules.imagefetcher.FetcherActor
import modules.imagefetcher.FetcherActor.{FetchRGB, FetchResponse}
import modules.scheduler.MonitoringActor.{GetProcess, UpdateProcess}
import modules.scheduler.SchedulerActor.{CancelProcessing, StartProcessing}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WSClient

import scala.concurrent.duration._

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

  implicit val timeout: Timeout = 15.seconds

  override def receive: Receive = {
    case StartProcessing(id) => startProcessing(id)
    case CancelProcessing(id) => cancelProcessing(id)
  }

  /**
    * Entry point of the pipeline
    * * Send query to the search engine
    * * Download image
    */
  def startProcessing(id: String): Unit = {
      monitoring.ask(GetProcess(id))
        .mapTo[Query]
        .map(query => query.details)
        .map {
          case Some(details) => details
          case _ =>
            monitoring ! UpdateProcess(id, "failed: no details")
            throw new Exception("no details")
        }
        .flatMap(details => fetcherActor.ask(FetchRGB("2015-01-01", details.place, Some(10), id)))
        .mapTo[FetchResponse]
        .foreach(response => println("Url from falsk server : " + response.url))
  }

  /**
    * Remove a query from the query list (if processing hasn't started yet)
    */
  def cancelProcessing(id: String): Unit = {
    val index = processes.indexWhere(query => query.id == id)
    processes.remove(index)
  }


}
