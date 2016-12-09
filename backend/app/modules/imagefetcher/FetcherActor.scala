package modules.imagefetcher


import akka.actor.{Actor, Props}
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient

import play.api.libs.concurrent.Execution.Implicits.defaultContext


object FetcherActor {

  /**
    * @param ws WebService to send http request to the flask server
    * @param serverUrl Url of the flask server
    */
  def props(ws: WSClient, serverUrl: String) = Props(new FetcherActor(ws, serverUrl))

  /**
    * Messages definitions
    */
  case class FetchRGB(start: String, delta: String, scale: Option[Double], polygon: JsValue)
  case class FetchResponse(url: String)
}

class FetcherActor(ws: WSClient, serverUrl: String) extends Actor {

  /**
    * Import implicit definition
    */
  import FetcherActor._

  /**
    * Message handling
    */
  override def receive: Receive = {
    case message: FetchRGB => fetchImage(message)
    case _ => sender() ! "Image Fetcher not yet implemented"
  }

  /**
    * Request the flask server to process image from Earth Engine and return the url to download the image
    *
    * @return
    */
  def fetchImage(message: FetchRGB) = {

    val initialParams = Seq(
      ("start", message.start),
      ("delta", message.delta),
      ("polygon", message.polygon.toString))

    val params =
      if (message.scale.isDefined) initialParams :+ ("scale", message.scale.get.toString)
      else initialParams

    val request = ws.url(serverUrl + "/rgb")
        .withQueryString(params:_*)
        .get()

    request.map ( response =>
        if (response.status == 200) {
          val url = (response.json \ "href").as[String]
          sender ! FetchResponse(url)
        } else {
          val error = (response.json \ "error").asOpt[String]
          sender ! "An error occurred during image fetching: " + error.getOrElse("no details")
        }
    )
  }

}
