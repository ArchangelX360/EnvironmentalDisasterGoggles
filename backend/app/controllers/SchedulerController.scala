package controllers

import javax.inject.Inject

import akka.pattern.ask
import akka.util.Timeout
import models.Query
import modules.scheduler.MonitoringActor._
import modules.scheduler.SchedulerService
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

import scala.collection.mutable
import scala.concurrent.duration._
import models.Query._
import modules.scheduler.SchedulerActor.{CancelProcessing, StartProcessing}

/**
  * Handle all request from the client to the scheduler
  */
class SchedulerController @Inject() (schedulerService: SchedulerService) extends Controller {

  /**
    * Reference to the monitoring actor
    */
  private val monitoringActor = schedulerService.monitoringActor

  /**
    * Reference to the scheduler actor
    */
  private val schedulerActor = schedulerService.schedulerActor

  /**
    * After this delay the server send a timeout, however the process is still running
    */
  implicit val timeout: Timeout = 5.seconds

  /**
    *
    * @return The list of all tasks executed by the server
    */
  def getProcess = Action.async {
    (monitoringActor ? ListProcess())
      .mapTo[Queries]
      .map(process => Ok(Json.toJson(process)))
  }

  /**
    * Start fetching images and apply processing from a processed query
    */
  def startProcessing(id: String) = Action {
    schedulerActor ! StartProcessing(id)
    Ok("started")
  }

  /**
    * Remove the query from the query list
    */
  def cancelProcessing(id: String) = Action {
    schedulerActor ! CancelProcessing(id)
    Ok("stopped")
  }
}
