package modules.imagefetcher

import java.net.{URI, URL}

import akka.actor.{Actor, Props}
import modules.imagefetcher.FetcherActor.{FetchMessage, FetchResponse}
import play.api.Configuration
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient

import scala.concurrent.Await
import scala.concurrent.duration.Duration


object FetcherActor {

  /**
    * @param ws WebService to send http request to the flask server
    * @param serverUrl Url of the flask server
    */
  def props(ws: WSClient, serverUrl: String) = Props(new FetcherActor(ws, serverUrl))

  /**
    * Messages definitions
    */
  case class FetchMessage(start: String, end: String, position: JsValue)
  case class FetchResponse(url: String)
}

class FetcherActor(ws: WSClient, serverUrl: String) extends Actor {

  /**
    * Message handling
    */
  override def receive: Receive = {
    case FetchMessage(start, end, position) => sender ! fetchImage(start, end, position)
    case _ => sender() ! "Image Fetcher not yet implemented"
  }

  /**
    * Request the flask server to process image from Earth Engine and return the url to download the image
    * @return
    */
  def fetchImage(start: String, end: String, position: JsValue): FetchResponse = {

    val request = ws.url(serverUrl + "/rgb")
        .withQueryString(
          ("start", start),
          ("end", end),
          ("position", position.toString()))
          .get()


    val url = Await.result(request, Duration.Inf).body
    FetchResponse(url)
  }

}
