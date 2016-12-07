name := "EnvironmentalDisasterGoggles"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies += filters

lazy val root = (project in file(".")).enablePlugins(PlayScala)
    