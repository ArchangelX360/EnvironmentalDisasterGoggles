package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.util.Timeout
import modules.sparql.SparQLActor
import modules.sparql.SparQLActor.{FetchAllGeoJSON, FetchEventClasses, InsertEvent, SparQLFetchResponse}
import play.api.libs.json.{JsNumber, JsString, JsValue}
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}

import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

// FIXME(archangel): this is not supposed to be a real controller

/**
  * This controller exist for debug purposes, this give a direct access to the image fetcher service
  */
class SparQLController @Inject()(system: ActorSystem,
                                 ws: WSClient,
                                 configuration: play.api.Configuration)
  extends Controller {
  /**
    * After this delay the server send a timeout, however the process is still running
    */
  implicit val timeout: Timeout = configuration.underlying.getInt("pfs.servers.imageFetcherTimeout").seconds

  val fetcherActor = system.actorOf(SparQLActor.props(ws,
    configuration.underlying.getString("pfs.servers.fusekiServerUrl"),
    configuration.underlying.getString("pfs.servers.fusekiDBName")))

  def fetchAllGeoJSON = Action.async { _ =>
    val response = fetcherActor ? FetchAllGeoJSON()
    response.map {
      case SparQLFetchResponse(stringArray) => Ok(stringArray.mkString(","))
      case error => InternalServerError(error.toString)
    }
  }

  def fetchEventClasses = Action.async { _ =>
    val response = fetcherActor ? FetchEventClasses()
    response.map {
      case SparQLFetchResponse(stringArray) => Ok(stringArray.mkString(","))
      case error => InternalServerError(error.toString)
    }
  }

  def insertEvent = Action.async { request =>
    val body = request.body.asJson.get
    val response = fetcherActor ? InsertEvent(
      startDate = (body \ "startDate").as[String],
      endDate = (body \ "endDate").as[String],
      eventClass = (body \ "eventClass").as[String],
      algorithm = (body \ "algorithm").as[String],
      imageLinks = (body \ "imageLinks").as[Array[String]],
      geoJson = (body \ "geoJson").as[JsValue]
    )

    response.map {
      case SparQLFetchResponse(stringArray) => Ok(stringArray.mkString(","))
      case error => InternalServerError(error.toString)
    }
  }

}