package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import modules.imagefetcher.FetcherActor
import play.api.mvc.{Action, Controller, Result}
import akka.pattern.ask
import akka.util.Timeout
import modules.imagefetcher.FetcherActor.{FetchRGB, FetchResponse}
import play.api.libs.ws.WSClient

import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits.defaultContext


/**
  * This controller exist for debug purposes, this give a direct access to the image fetcher service
  */
class FetcherController @Inject() (system: ActorSystem, ws: WSClient, configuration: play.api.Configuration) extends Controller {

  val fetcherActor = system.actorOf(FetcherActor.props(ws, configuration.underlying.getString("pfs.servers.imageFetcherUrl")))

  /**
    * After this delay the server send a timeout, however the process is still running
    */
  implicit val timeout: Timeout = configuration.underlying.getInt("pfs.servers.imageFetcherTimeout").seconds

  /**
    * Ask the fetcher actor for an image and return an url to download a zip of this image
    */
  def fetchRGB = Action.async { request =>
    val body = request.body.asJson.get
    val response = fetcherActor ? FetchRGB (
      start = (body \ "start").as[String],
      delta = (body \ "delta").as[String],
      scale = (body \ "scale").asOpt[Double],
      polygon = (body \ "position").get
    )

    response.map {
      case FetchResponse(url) => Ok(url)
      case error => InternalServerError(error.toString)
    }
  }

}
