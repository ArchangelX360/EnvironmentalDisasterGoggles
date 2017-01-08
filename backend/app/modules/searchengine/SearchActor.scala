package modules.searchengine

import java.security.InvalidParameterException

import akka.actor.{Actor, Props}
import akka.pattern.ask
import akka.util.Timeout
import models.{Query, Task}
import modules.scheduler.MonitoringActor.{StartProcess, StartTask, UpdateTask}
import modules.scheduler.SchedulerService
import modules.sparql.SparQLActor.{FetchEventClasses, SparQLEventClassesResponse}
import org.joda.time.Instant
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{Json, Writes}

import scala.concurrent.duration._

object SearchActor {
  def props(schedulerService: SchedulerService) = Props(new SearchActor(schedulerService))

  case class SearchMessage(message: String, author: String)
  case class SearchDetails(place: String, from: Instant, to: Instant, event:String)

  implicit val instantWriters: Writes[Instant] = Writes.apply(time => Json.toJson(time.toString))
  implicit val responseWriters: Writes[SearchDetails] = Json.writes[SearchDetails]
}

class SearchActor (schedulerService: SchedulerService) extends Actor {

  private val monitoring = schedulerService.monitoringActor

  private val sparql = schedulerService.sparQLActor

  /**
    * Import implicit definition from companion object
    */
  import SearchActor._

  /**
    * after this delay the server send a timeout, however the process is still running
    */
  implicit val timeout: Timeout = 10.minutes

  /**
    * Handle incoming message send to this actor
    */
  override def receive: Receive = {
    case SearchMessage(message, author) => processMessage(message, author)
    case _ => throw new InvalidParameterException()
  }

  /**
    * Handle search message, send it to spotlight for TAL and contact other actors to schedule the processing
    *
    * @param message Search query send by the web interface
    * @return A confirmation that the message is scheduled for processing
    */
  def processMessage(message: String, author: String) = {

    val currentSender = sender

    val messageInLowerCase = message.toLowerCase

    val event = sparql
      .ask(FetchEventClasses())
      .mapTo[SparQLEventClassesResponse]
      .map(sparqlResponse => sparqlResponse.classes)
      .map(events => events.find(e => messageInLowerCase.contains(e.toLowerCase)))

    val parser = new NLPParser(message)

    val dates = parser.extractDate()

    val place = parser.extractPlace().mkString(" ")

    val details =  SearchDetails(
      place = place,
      from = dates.headOption.getOrElse(Instant.now()),
      to = if (dates.isEmpty) Instant.now() else dates.tail.headOption.getOrElse(Instant.now()),
      event = "")

    val fullDetails = event map (event => details.copy(event = event.getOrElse("noEventParsed")))

    val schedulerResponse = fullDetails.flatMap( details =>
      monitoring ? StartProcess(message, author, Some(details))
    ).mapTo[Query]

    schedulerResponse map (query => {
      monitoring.ask(StartTask(query.id, "Search task"))
        .mapTo[Task]
        .foreach(task => monitoring ? UpdateTask(query.id, task.id, Some("completed"), Some(100)))

      currentSender ! query
    })

  }
}


