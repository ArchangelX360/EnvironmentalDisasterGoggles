package models

case class Task(id: String, name: String, status: String, progress: Int)

case class Process(id: String, query: String, status: String, tasks: List[Task]) {

}
