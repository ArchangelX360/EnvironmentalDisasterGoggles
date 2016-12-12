name := "EnvironmentalDisasterGoggles"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  filters,
  ws,
  "edu.stanford.nlp" % "stanford-corenlp" % "3.6.0",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.6.0" classifier "models"
)

lazy val root = (project in file(".")).enablePlugins(PlayScala)
    