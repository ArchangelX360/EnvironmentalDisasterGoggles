package modules.imagefetcher

import java.net.URL

import akka.actor.{Actor, Props}
import modules.imagefetcher.FetcherActor.{FetchMessage, FetchResponse}
import play.api.libs.json.JsValue


object FetcherActor {
  def props = Props[FetcherActor]

  /**
    * Messages definitions
    */
  case class FetchMessage(start: String, end: String, position: JsValue)
  case class FetchResponse(url: String)
}

class FetcherActor extends Actor {

  /**
    * Url of the flask server (python) for downloading image
    */
  val serverUrl = new URL("localhost:2000")

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
    val url = scala.io.Source.fromURL(serverUrl).mkString
    FetchResponse(url)
  }

}
