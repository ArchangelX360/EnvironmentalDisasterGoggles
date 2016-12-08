package controllers

import javax.inject.Inject

import akka.actor.ActorSystem
import modules.imagefetcher.FetcherActor
import play.api.mvc.{Action, Controller}
import akka.pattern.ask
import akka.util.Timeout
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
  implicit val timeout: Timeout = 5.seconds


  /**
    * Ask the fetcher actor for an image an return an url to download that image
    */
  def fetch = Action.async { request =>
    val body = request.body.asJson.get
    (fetcherActor ? FetcherActor.FetchMessage(
            start = (body \ "start").as[String],
            end = (body \ "end").as[String],
            position = (body \ "position").get
      ))
      .mapTo[FetcherActor.FetchResponse]
      .map(response => Ok(response.url))
  }

}
