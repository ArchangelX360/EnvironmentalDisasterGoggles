package modules.scheduler

import akka.actor.{ActorRef, ActorSystem}
import com.google.inject.{Inject, Singleton}
import models.Query.Queries
import play.api.libs.ws.WSClient

import scala.collection.mutable.ListBuffer

@Singleton
class SchedulerService @Inject() (system: ActorSystem, ws: WSClient, configuration: play.api.Configuration) {

  /**
    * List of all the process run by the server (including finished ones)
    */
  private val processes: Queries = ListBuffer.empty

  /**
    * The monitoring actor have access to the status of all tasks
    */
  val monitoringActor: ActorRef = system.actorOf(MonitoringActor.props(processes))


  /**
    * The scheduler actor manage the lifecycle of tasks within a query
    */
  val schedulerActor: ActorRef = system.actorOf(SchedulerActor.props(processes, configuration, ws, monitoringActor))


}
