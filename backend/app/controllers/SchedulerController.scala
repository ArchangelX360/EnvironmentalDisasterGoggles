package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import com.google.inject.Singleton
import modules.scheduler.{SchedulerActor, SchedulerService}
import play.api.mvc.{Action, Controller}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext


/**
  * Handle all request from the client to the scheduler
  */
class SchedulerController @Inject() (schedulerService: SchedulerService) extends Controller {

  /**
    * Reference to the scheduler actor
    */
  val schedulerActor = schedulerService.schedulerActor

  /**
    * After this delay the server send a timeout, however the process is still running
    */
  implicit val timeout: Timeout = 5.seconds

  /**
    *
    * @return The list of all tasks executed by the server
    */
  def getProcess() = Action.async {
    (schedulerActor ? "List").mapTo[String].map(process => Ok(process))
  }

}
