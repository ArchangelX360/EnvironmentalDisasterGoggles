package modules.imagefetcher


import java.io.{File, FileOutputStream}

import akka.actor.{Actor, ActorRef, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.util.ByteString
import modules.scheduler.MonitoringActor.StartTask
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WSClient


object FetcherActor {

  /**
    * @param ws WebService to send http request to the flask server
    * @param serverUrl Url of the flask server
    */
  def props(ws: WSClient, serverUrl: String, monitoring: ActorRef) = Props(new FetcherActor(ws, serverUrl, monitoring))

  /**
    * Messages definitions
    */
  case class FetchRGB(date: String, place: String, scale: Option[Double], queryId: String)
  case class FetchResponse(url: String)
  case class DownloadFile(url: String)
  case class Downloaded(file: File)
}

class FetcherActor(ws: WSClient, serverUrl: String, monitoring: ActorRef) extends Actor {

  /**
    * Import implicit definition
    */
  import FetcherActor._
  implicit val materializer = ActorMaterializer()

  /**
    * Message handling
    */
  override def receive: Receive = {
    case message: FetchRGB => fetchImage(message)
    case DownloadFile(url) => downloadFile(url)
    case _ => sender() ! "Image Fetcher not yet implemented"
  }

  /**
    * Request the flask server to process image from Earth Engine and return the url to download the image
    */
  def fetchImage(message: FetchRGB) = {

    monitoring ! StartTask(message.queryId, "Fetching image")

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

  def downloadFile(url: String) = {

    val currentSender = sender

    val file = new File("Downloaded")
    val response = ws.url(url).withMethod("GET").stream()

    val downloadedFile = response.flatMap(res => {
      val outputStream = new FileOutputStream(file)

      val sink = Sink.foreach[ByteString](bytes => outputStream.write(bytes.toArray))

      res.body.runWith(sink).andThen {
        case result =>
          outputStream.close()
          result.get
      }.map(_ => file)

    })

    downloadedFile map (downloaded => sender ! Downloaded(downloaded))
  }

}
