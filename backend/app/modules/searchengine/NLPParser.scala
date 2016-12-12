package modules.searchengine

import java.time.LocalDateTime
import java.util.Properties

import edu.stanford.nlp.ling.CoreAnnotations
import edu.stanford.nlp.ling.CoreAnnotations._
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.time.{TimeAnnotations, TimeAnnotator, TimeExpression}

import scala.collection.JavaConverters._

case class Token(word: String, nef: String, norm: String)

/**
  * This class use Stanford NLP library to parse a string and extract metadata such as
  * dates and places
  *
  * Only models for english are loaded
  */
class NLPParser(query: String) {

  /**
    * The pipeline defines all the operations applied to the query
    */
  val pipelineProperties = new Properties()
  pipelineProperties.setProperty("annotators", "tokenize, ssplit, pos, lemma, ner")
  val pipeline = new StanfordCoreNLP(pipelineProperties)

  /**
    * Sutime is a plugin for date comprehension
    */
  pipeline.addAnnotator(new TimeAnnotator("sutime", new Properties()))

  /**
    * Create and annotate the query
    */
  val document = new Annotation(query)
  document.set(classOf[CoreAnnotations.DocDateAnnotation], LocalDateTime.now().toString)
  pipeline.annotate(document)

  /**
    * Entities extraction is computed lazily to avoid extra computation if only dates are required
    */
  private lazy val entities = for (
    sentences <- document.get(classOf[SentencesAnnotation]).asScala;
    token <- sentences.get(classOf[TokensAnnotation]).asScala
  ) yield Token(
    token.get(classOf[TextAnnotation]),
    token.get(classOf[NamedEntityTagAnnotation]),
    token.get(classOf[NormalizedNamedEntityTagAnnotation])
  )

  private lazy val times = for (
    cm <- document.get(classOf[TimeAnnotations.TimexAnnotations]).asScala;
    time = cm.get(classOf[TimeExpression.Annotation]).getTemporal
  ) yield time

  /**
    * Extract dates from the query, the date extracted are formated following the TIMEX3 specification
    */
  def extractDate(): List[String] = times.map(_.toString).toList

  def extractPlace(): List[String] = entities
    .filter(token => token.nef == "PLACE")
    .map(token => token.word)
    .distinct
    .toList

}
