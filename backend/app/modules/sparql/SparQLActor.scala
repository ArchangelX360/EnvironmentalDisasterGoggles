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
    * @param ws           WebService to send http request to the fuseki server
    * @param serverUrl    Url of the fuseki server
    * @param databaseName fuseki database name
    */
  def props(ws: WSClient, serverUrl: String, databaseName: String) =
    Props(new SparQLActor(ws, serverUrl, databaseName))

  /*
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

  case class SparQLInsertResponse(message: String)

}

class SparQLActor(ws: WSClient, serverUrl: String,
                  databaseName: String) extends Actor {

  /*
    * Import implicit definition
    */

  import SparQLActor._

  val envePrefix = "PREFIX enve: <http://www.semanticweb.org/archangel/ontologies/2016/11/environmental-events#> \n"
  val rdfsPrefix = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#> \n"
  val rdfPrefix = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n"
  val xsdPrefix = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n"

  /**
    * Message handler
    */
  override def receive: Receive = {
    case _: FetchEventClasses => fetchEventClasses()
    case _: FetchAllGeoJSON => fetchAllGeoJSON()
    case message: InsertEvent => insertEvent(message)
    case _ => throw new InvalidParameterException()
  }

  /**
    * Fetches all geoJSON strings in the fuseki server's ontology
    */
  def fetchAllGeoJSON(): Unit = {
    val geoJsonRequest = envePrefix + rdfsPrefix +
      "SELECT ?object WHERE {?subject enve:geoJson ?object}"

    def parseGeoJson(response: WSResponse): Array[String] = {
      val resJson = (response.json \ "results" \ "bindings").as[Array[JsObject]]
      resJson.map(x => (x \ "object" \ "value").as[String])
    }

    sendResponse(executeQuery(geoJsonRequest), parseGeoJson)
  }

  /**
    * Fetches all event classes of the fuseki server's ontology
    * Example: Deforestation, Urbanization
    */
  def fetchEventClasses(): Unit = {
    val eventClassesRequest = envePrefix + rdfsPrefix +
      "SELECT ?subject ?predicate ?object WHERE {?subject rdfs:subClassOf enve:Event}"

    def parseEventClasses(response: WSResponse): Array[String] = {
      val resJson = (response.json \ "results" \ "bindings").as[Array[JsObject]]
      resJson.map(x => (x \ "subject" \ "value").as[String].split("#")(1))
    }

    sendResponse(executeQuery(eventClassesRequest), parseEventClasses)
  }

  /**
    * Inserts an event in the fuseki server's ontology
    *
    * @param event event object containing event parameters
    */
  def insertEvent(event: InsertEvent): Unit = {
    // FIXME: security flaw, this should be fixed using Jena Java library as shown here to create queries: https://morelab.deusto.es/code_injection/

    var insertRequest = envePrefix + rdfsPrefix + rdfPrefix + xsdPrefix +
      "INSERT DATA\n" +
      "{\n"
    // TODO(archangel): change random value to a better one
    insertRequest += "enve:event" + Random.nextInt() + " rdf:type enve:" + event.eventClass + " ;\n"
    insertRequest += "enve:startDate \"" + event.startDate + "\"^^xsd:dateTime ;\n"
    insertRequest += "enve:endDate \"" + event.endDate + "\"^^xsd:dateTime ;\n"
    insertRequest += "enve:algorithm enve:" + event.algorithm + " ;\n"

    event.imageLinks.foreach(link => {
      insertRequest += "enve:imageLink \"" + link + "\"^^xsd:anyURI ;\n"
    })
    insertRequest += "enve:geoJson \"" + event.geoJson.toString().replace("\"", "\\\"") + "\"\n"

    insertRequest += "}"

    val currentSender = sender

    val params = Map("update" -> Seq(insertRequest))

    val request = ws.url(serverUrl + "/" + databaseName + "/update")
      .post(params)

    request.map(response =>
      if (response.status == 200) {
        currentSender ! SparQLInsertResponse(response.statusText)
      } else {
        val error = (response.json \ "error").asOpt[String]
        currentSender ! "An error occurred during image fetching: " + error.getOrElse("no details")
      }
    )
  }

  /**
    * Executes a "query" SparQL request on the fuseki server using POST
    *
    * @param sparqlRequest the SparQL request we want to execute
    * @return the request response's future
    */
  def executeQuery(sparqlRequest: String): Future[WSResponse] =
    ws.url(serverUrl + "/" + databaseName + "/query")
      .withQueryString("query" -> sparqlRequest)
      .get()

  /**
    * Parses and sends the response to the sender
    *
    * @param request the resquest response's future we executed
    * @param parser  the response's parser
    */
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
