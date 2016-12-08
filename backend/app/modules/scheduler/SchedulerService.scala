package modules.scheduler

import akka.actor.ActorSystem
import com.google.inject.{Inject, Singleton}

@Singleton
class SchedulerService @Inject() (system: ActorSystem) {

  /**
    * The scheduler have access to the status of all tasks
    */
  val monitoringActor = system.actorOf(MonitoringActor.props)


}
