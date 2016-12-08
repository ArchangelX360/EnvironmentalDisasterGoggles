package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import models.Process
import modules.scheduler.SchedulerService
import modules.searchengine.SearchActor
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.{JsObject, Json}
import play.api.mvc.{Action, Controller}

import scala.concurrent.duration._

/**
  * Handle all the query from the searchbar
  */
class SearchController @Inject() (system: ActorSystem, schedulerService: SchedulerService) extends Controller {

  /**
    * Handle all the search requests (including NLP processing)
    */
  val searchActor = system.actorOf(SearchActor.props(schedulerService.schedulerActor), "search-actor")

  /**
    * Do no send a response after this delay, the processing is not canceled anyway
    */
  implicit val timeout: Timeout = 5.seconds

  /**
    * Forward the message to the search actor and notify the sender that the processing have been scheduled
    */
  def search() = Action.async { request =>

    /**
      * Extract the parameter query from the request body
      */
    val message = request.body.asJson
      .flatMap(js => (js \ "query").asOpt[String])

    /**
      * Send the query to the search actor
      */
    (searchActor ? SearchActor.SearchMessage(message.getOrElse("empty")))
          .mapTo[Process]
          .map(resultat => Ok(
            JsObject(Seq(
              ("status", Json.toJson(resultat.status)),
              ("query", Json.toJson(resultat.query)),
              ("place", Json.toJson("Pau")),
              ("from", Json.toJson("2007-04-05T14:30Z")),
              ("to", Json.toJson("2008-04-05T14:30Z")),
              ("type", Json.toJson("Fire"))
            )
          )))


  }
}

