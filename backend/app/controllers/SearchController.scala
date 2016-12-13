package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import modules.scheduler.SchedulerService
import modules.searchengine.SearchActor
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc.{Action, Controller}

import scala.concurrent.duration._

/**
  * Handle all the query from the searchbar
  */
class SearchController @Inject() (system: ActorSystem, schedulerService: SchedulerService) extends Controller {

  import SearchActor._

  /**
    * Handle all the search requests (including NLP processing)
    */
  val searchActor = system.actorOf(SearchActor.props(schedulerService.monitoringActor), "search-actor")

  /**
    * Do no send a response after this delay, the processing is not canceled anyway
    */
  implicit val timeout: Timeout = 15.seconds

  /**
    * Forward the message to the search actor and notify the sender that the processing have been scheduled
    */
  def search() = Action.async { request =>

    /**
      * Extract the parameter query from the request body
      */
    val body = request.body.asJson
    val message = body.flatMap(js => (js \ "query").asOpt[String])
    val author = body.flatMap(js => (js \ "author").asOpt[String])

    /**
      * Send the query to the search actor
      */
    (searchActor ? SearchActor.SearchMessage(message.getOrElse("empty query"), author.getOrElse("empty author")))
          .mapTo[SearchResponse]
          .map(response => Ok(Json.toJson(response)))

  }

}


