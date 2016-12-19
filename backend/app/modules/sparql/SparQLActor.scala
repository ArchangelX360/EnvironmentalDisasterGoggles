package modules.sparql

import java.security.InvalidParameterException

import akka.actor.{Actor, Props}
import play.api.libs.json.{JsObject, JsValue}
import play.api.libs.ws.{WSClient, WSResponse}

import scala.concurrent.Future
import scala.util.Random
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object SparQLActor {

  /**
    * @param ws        WebService to send http request to the fuseki server
    * @param serverUrl Url of the fuseki server
    */
  def props(ws: WSClient, serverUrl: String, databaseName: String) =
    Props(new SparQLActor(ws, serverUrl, databaseName))

  /**
    * Messages definitions
    */

  /**
    * @param eventClass type of event as rdf:type
    * @param algorithm  algorithm used as enve:Algorithm
    * @param startDate  start date string formatted as xsd:DateTime
    * @param endDate    end date string formatted as xsd:DateTime
    * @param imageLinks array of image link formatted as xsd:anyURI
    * @param geoJson    geoJson of the area
    */
  case class InsertEvent(eventClass: String, algorithm: String,
                         startDate: String, endDate: String,
                         imageLinks: Array[String], geoJson: JsValue)

  case class FetchAllGeoJSON()

  case class FetchEventClasses()

  case class SparQLFetchResponse(response: Array[String])
}

class SparQLActor(ws: WSClient, serverUrl: String,
                  databaseName: String) extends Actor {

  /**
    * Import implicit definition
    */

  import SparQLActor._

  /**
    * Message handling
    */
  override def receive: Receive = {
    case _: FetchEventClasses => fetchEventClasses()
    case _: FetchAllGeoJSON => fetchAllGeoJSON()
    case message: InsertEvent => insertEvent(message)
    case _ => throw new InvalidParameterException()
  }

  def fetchAllGeoJSON(): Unit = {
    val geoJsonRequest =
      "PREFIX enve: <http://www.semanticweb.org/archangel/ontologies/2016/11/environmental-events#> \n" +
        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n" +
        "SELECT ?object WHERE {?subject enve:geoJson ?object}"

    def parseGeoJson(response: WSResponse): Array[String] = {
      val resJson = (response.json \ "results" \ "bindings").as[Array[JsObject]]
      resJson.map(x => (x \ "object" \ "value").as[String])
    }

    sendResponse(executeQuery(geoJsonRequest), parseGeoJson)
  }

  def fetchEventClasses(): Unit = {
    val eventClassesRequest =
      "PREFIX enve: <http://www.semanticweb.org/archangel/ontologies/2016/11/environmental-events#> \n" +
        "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n " +
        "SELECT ?subject ?predicate ?object WHERE {?subject rdfs:subClassOf enve:Event}"

    def parseEventClasses(response: WSResponse): Array[String] = {
      val resJson = (response.json \ "results" \ "bindings").as[Array[JsObject]]
      resJson.map(x => (x \ "subject" \ "value").as[String].split("#")(1))
    }

    sendResponse(executeQuery(eventClassesRequest), parseEventClasses)
  }

  def insertEvent(message: InsertEvent): Unit = {
    var insertRequest = "PREFIX enve: <http://www.semanticweb.org/archangel/ontologies/2016/11/environmental-events#>\n" +
      "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
      "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
      "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
      "\n" +
      "INSERT DATA\n" +
      "{\n"
    // TODO(archangel): change random value to a better one
    insertRequest += "enve:event" + Random.nextInt() + " rdf:type enve:" + message.eventClass + " ;\n"
    insertRequest += "enve:startDate \"" + message.startDate + "\"^^xsd:dateTime ;\n"
    insertRequest += "enve:endDate \"" + message.endDate + "\"^^xsd:dateTime ;\n"
    insertRequest += "enve:algorithm enve:" + message.algorithm + " ;\n"

    message.imageLinks.foreach(link => {
      insertRequest += "enve:imageLink \"" + link + "\"^^xsd:anyURI ;\n"
    })
    insertRequest += "enve:geoJson \"" + message.geoJson.toString().replace("\"", "\\\"") + "\"\n"

    insertRequest += "}"

    def parseInsert(response: WSResponse): Array[String] = {
      Array(response.statusText) // TODO: better response eventually
    }

    sendResponse(executeUpdate(insertRequest), parseInsert)
  }

  def executeUpdate(sparqlRequest: String): Future[WSResponse] = {
    val params = Map("update" -> Seq(sparqlRequest))

    ws.url(serverUrl + "/" + databaseName + "/update")
      .post(params)
  }

  def executeQuery(sparqlRequest: String): Future[WSResponse] =
    ws.url(serverUrl + "/" + databaseName + "/query")
      .withQueryString("query" -> sparqlRequest)
      .get()

  def sendResponse(request: Future[WSResponse], parser: WSResponse => Array[String]) {
    val currentSender = sender

    request.map(response =>
      if (response.status == 200) {
        currentSender ! SparQLFetchResponse(parser(response))
      } else {
        val error = (response.json \ "error").asOpt[String]
        currentSender ! "An error occurred during image fetching: " + error.getOrElse("no details")
      }
    )
  }

}
