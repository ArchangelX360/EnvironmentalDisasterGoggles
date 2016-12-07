package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import com.google.inject.Singleton
import modules.scheduler.SchedulerActor
import play.api.mvc.{Action, Controller}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext


@Singleton
class SchedulerController @Inject() (system: ActorSystem) extends Controller {

  val schedulerActor = system.actorOf(SchedulerActor.props)

  implicit val timeout: Timeout = 5.seconds

  def getProcess() = Action.async {
    (schedulerActor ? "List").mapTo[String].map(process => Ok(process))
  }

}
