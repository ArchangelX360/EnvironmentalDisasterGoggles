package modules.imagefetcher


import akka.actor.{Actor, ActorRef, Props}
import play.api.libs.json.JsValue
import play.api.libs.ws.WSClient
import play.api.libs.concurrent.Execution.Implicits.defaultContext


object FetcherActor {

  /**
    * @param ws WebService to send http request to the flask server
    * @param serverUrl Url of the flask server
    */
  def props(ws: WSClient, serverUrl: String, scheduler: ActorRef) = Props(new FetcherActor(ws, serverUrl, scheduler))

  /**
    * Messages definitions
    */
  case class FetchRGB(date: String, place: String, scale: Option[Double])
  case class FetchResponse(url: String)
}

class FetcherActor(ws: WSClient, serverUrl: String, scheduler: ActorRef) extends Actor {

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
    */
  def fetchImage(message: FetchRGB) = {

    val initialParams = Seq(
      ("date", message.date),
      ("place", message.place))

    val params =
      if (message.scale.isDefined) initialParams :+ ("scale", message.scale.get.toString)
      else initialParams

    val request = ws.url(serverUrl + "/rgb")
        .withQueryString(params:_*)
        .get()

    val currentSender = sender

    request.map ( response =>
        if (response.status == 200) {
          val url = (response.json \ "href").as[String]
          currentSender ! FetchResponse(url)
        } else {
          val error = (response.json \ "error").asOpt[String]
          currentSender ! "An error occurred during image fetching: " + error.getOrElse("no details")
        }
    )
  }

}
