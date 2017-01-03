package models

import modules.searchengine.SearchActor.SearchDetails
import play.api.libs.json._

import scala.collection.mutable
import scala.collection.mutable.ListBuffer

/**
  * A tasks is a process realised by an actor toward the creation of a response to a query
  */
case class Task(id: String, name: String, status: String, progress: Int, metadata: Map[String, String])

/**
  * Represent a query send by the user
  * @param name Content of the query (request)
  * @param author Token for author authentication
  * @param status Task progression
  * @param tasks List all tasks associated with this query
  */
case class Query(id: String, details: Option[SearchDetails], author: String, name: String, status: String, tasks: ListBuffer[Task], metadata: Map[String, String])

/**
  * Companion object for Query class, contains implicit converters for tasks and query
  */
object Query {
  implicit val taskWriters: Writes[Task] = Json.writes[Task]
  implicit val processWriters: Writes[Query] = Json.writes[Query]
  type Queries = mutable.ListBuffer[Query]
}

