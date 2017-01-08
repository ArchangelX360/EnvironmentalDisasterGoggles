package modules.sparql

import java.security.InvalidParameterException

import akka.actor.{Actor, Props}
import play.api.libs.json.{JsObject}
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
    * @param place      place where the event took place
    * @param imageLinks array of image link formatted as xsd:anyURI
    */
  case class OntologyEvent(eventClass: String, algorithm: String,
                           startDate: String, endDate: String,
                           place: String,
                           imageLinks: Array[String])

  case class FetchEventClasses()

  /**
    * @param startDate  start date string formatted as xsd:DateTime
    * @param endDate    end date string formatted as xsd:DateTime
    * @param eventClass event class formatted as a string like "Deforestation" or "Urbanisation"
    * @param place      place where the event took place
    * @param algorithm  algorithm used like "RGB" or "Photosynthesis"
    */
  case class CacheParameters(eventClass: String,  algorithm: String, startDate: String, endDate: String,  place: String)

  /**
    * @param uris array of uri strings of already processed images for the given query
    */
  case class SparQLCachedResponse(uris: Array[String])

  /**
    * @param classes array of event classes string
    */
  case class SparQLEventClassesResponse(classes: Array[String])

  /**
    * @param message response status or error message
    */
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
    case parameters: CacheParameters => fetchCacheContent(parameters)
    case event: OntologyEvent => insertEvent(event)
    case _ => throw new InvalidParameterException()
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
    * Parse event classes response
    *
    * @param response the fetching event classes query response
    * @return an array of event classes
    */
  private def parseEventClasses(response: WSResponse): Array[String] = {
    val resJson = (response.json \ "results" \ "bindings").as[Array[JsObject]]
    resJson.map(x => (x \ "eventClassName" \ "value").as[String].split("#")(1))
  }

  /**
    * Fetches all event classes of the fuseki server's ontology
    * and returns an array of event classes to the sender
    *
    * Example: Array(Deforestation, Urbanization)
    */
  def fetchEventClasses(): Unit = {

    val query = envePrefix + rdfsPrefix +
      "SELECT ?eventClassName WHERE {?eventClassName rdfs:subClassOf enve:Event}"

    val currentSender = sender

    executeQuery(query).map(response =>
      if (response.status == 200) {
        currentSender ! SparQLEventClassesResponse(parseEventClasses(response))
      } else {
        val error = (response.json \ "error").asOpt[String]
        currentSender ! "An error occurred during event class query execution: " + error.getOrElse("no details")
      }
    )
  }

  /**
    * Parse cache response
    *
    * @param response the fetching cachce query response
    * @return an array of uris string
    */
  private def parseCachedURIs(response: WSResponse): Array[String] = {
    val resJson = (response.json \ "results" \ "bindings").as[Array[JsObject]]
    resJson.map(x => (x \ "uri" \ "value").as[String])
  }

  /**
    * Fetches, if they exist, cached results corresponding to the specified parameters
    * and returns an array of uris to the sender
    *
    * @param parameters event parameters
    */
  def fetchCacheContent(parameters: CacheParameters): Unit = {

    var query = envePrefix + xsdPrefix +
      "SELECT ?uri\n" +
      "WHERE {\n" +
      "    ?event enve:imageLink ?uri .\n" +
      "    ?event enve:place \"" + parameters.place + "\" .\n" +
      "    ?event a enve:" + parameters.eventClass + " .\n" +
      "    ?event enve:startDate \"" + parameters.startDate + "\"^^xsd:dateTime .\n" +
      "    ?event enve:endDate \"" + parameters.endDate + "\"^^xsd:dateTime .\n" +
      "    ?event enve:algorithm enve:" + parameters.algorithm + "\n" +
      "}\n"

    val currentSender = sender

    executeQuery(query).map(response =>
      if (response.status == 200) {
        currentSender ! SparQLCachedResponse(parseCachedURIs(response))
      } else {
        val error = (response.json \ "error").asOpt[String]
        currentSender ! "An error occurred during cache fetching: " + error.getOrElse("no details")
      }
    )
  }

  /**
    * Inserts an event in the fuseki server's ontology
    * and return response status to the sender
    *
    * @param event event object containing event parameters
    */
  def insertEvent(event: OntologyEvent): Unit = {
    // FIXME: security flaw, this should be fixed using Jena Java library as shown here to create queries: https://morelab.deusto.es/code_injection/

    var insertRequest = envePrefix + rdfsPrefix + rdfPrefix + xsdPrefix
    insertRequest += "INSERT DATA\n"
    insertRequest += "{\n"
    insertRequest += "enve:event" + System.currentTimeMillis() + Random.nextInt() + " rdf:type enve:" + event.eventClass + " ;\n"
    insertRequest += "enve:startDate \"" + event.startDate + "\"^^xsd:dateTime ;\n"
    insertRequest += "enve:endDate \"" + event.endDate + "\"^^xsd:dateTime ;\n"
    insertRequest += "enve:algorithm enve:" + event.algorithm + " ;\n"
    insertRequest += "enve:place \"" + event.place + "\" ;\n"
    // TODO(archangel): use dbo:Place and enve:location edge instead of enve:place and string
    event.imageLinks.foreach(link => {
      insertRequest += "enve:imageLink \"" + link + "\"^^xsd:anyURI ;\n"
    })
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
        currentSender ! "An error occurred during event insertion: " + error.getOrElse("no details")
      }
    )
  }

}
