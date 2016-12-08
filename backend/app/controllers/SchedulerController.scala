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

/**
  * Handle all request from the client to the scheduler
  */
class SchedulerController @Inject() (schedulerService: SchedulerService) extends Controller {

  /**
    * Reference to the scheduler actor
    */
  val monitoringActor = schedulerService.monitoringActor

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
      .mapTo[mutable.MutableList[Query]]
      .map(process => Ok(Json.toJson(process)))
  }

}
