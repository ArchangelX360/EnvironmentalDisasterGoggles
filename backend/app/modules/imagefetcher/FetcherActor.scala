package modules.imagefetcher


import java.io.{File, FileOutputStream}

import akka.actor.{Actor, ActorRef, Props}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import akka.util.{ByteString, Timeout}
import modules.scheduler.MonitoringActor.{StartTask, UpdateTask}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.ws.WSClient
import akka.pattern.ask
import models.Task

import scala.concurrent.duration._
import scala.util.Random
import scala.concurrent.duration._

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
  case class DownloadFile(url: String, queryId: String)
  case class Downloaded(file: File)
}

class FetcherActor(ws: WSClient, serverUrl: String, monitoring: ActorRef) extends Actor {

  implicit val timeout: Timeout = 5.seconds

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
    case DownloadFile(url, queryId) => downloadFile(url, queryId)
    case _ => sender() ! "Image Fetcher not yet implemented"
  }

  /**
    * Request the flask server to process image from Earth Engine and return the url to download the image
    */
  def fetchImage(message: FetchRGB) = {

    val task = monitoring.ask(StartTask(message.queryId, "Fetching image"))(50.seconds).mapTo[Task]

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
          task map (t => monitoring ! UpdateTask(message.queryId, t.id, Some("Image generated on Earth Engine"), Some(100)))
          val url = (response.json \ "href").as[String]
          currentSender ! FetchResponse(url)
        } else {
          task map (t => monitoring ! UpdateTask(message.queryId, t.id, Some("Link generatation failed"), Some(0)))
          val error = (response.json \ "error").asOpt[String]
          currentSender ! "An error occurred during image fetching: " + error.getOrElse("no details")
        }
    )
  }

  /**
    * Download a file from an url through an akka stream an place it in the downloaded folder
    * (Downloaded file is not labeled yet e.g random name)
    */
  def downloadFile(url: String, processId: String) = {

    val task = monitoring.ask(StartTask(processId, "Downloading image")).mapTo[Task]

    val currentSender = sender

    val downloaded = new File("Downloaded").mkdir()
    val file = new File("Downloaded/" + Random.nextInt() + ".zip")
    val response = ws.url(url).withMethod("GET").stream()

    val downloadedFile = response.flatMap(res => {

      task.map(t => monitoring ! UpdateTask(processId, t.id, Some("Download started"), Some(20)))

      val outputStream = new FileOutputStream(file)

      val sink = Sink.foreach[ByteString](bytes => outputStream.write(bytes.toArray))

      res.body.runWith(sink).andThen {
        case result =>
          outputStream.close()
          task.map(t => monitoring ! UpdateTask(processId, t.id, Some("Download finished"), Some(100)))
          currentSender ! Downloaded(file)
      }

    })

  }

}
