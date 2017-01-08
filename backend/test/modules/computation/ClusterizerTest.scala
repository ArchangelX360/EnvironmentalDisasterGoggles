package modules.computation

import org.scalatestplus.play._
import play.api._
import play.api.inject.guice.GuiceApplicationBuilder

import scala.collection.mutable.ListBuffer

/**
  * Tests the modules.computation.Pixel class
  */
class ClusterizerTest extends PlaySpec {

  // We need to instance a guice application to load test resources
  val application: Application = new GuiceApplicationBuilder()
    .configure("some.configuration" -> "value")
    .build()

  "The Clusterizer loader" must {
    
    "loads correctly a custom generated image" in {
      val clusterizer = new Clusterizer(
        application.getFile("test/resources/simpleForestDiff.png"))

      clusterizer.width mustBe 20
      clusterizer.height mustBe 20
    }

    "loads correctly a real sample image" in {
      // Not throwing anything is enough for this test.
      val clusterizer = new Clusterizer(
        application.getFile("test/resources/realSampleForestDiff.png"))
    }

  }

  "The Clusterizer util" must {

    // Create a clusterizer instance for this test set
    val clusterizer = new Clusterizer(
      application.getFile("test/resources/simpleForestDiff.png"))

    "calculate correctly the percentage of deforestation" in {
      val nbRedPixels = 18.0
      clusterizer.percents mustBe (nbRedPixels / (20.0 * 20.0))
    }

    "get the pixels matching the threshold" in {
      // The test sample contains 18 pixels matching the threshold
      val expectedPixels = List[Pixel](
        Pixel(1, 1, 0xff0000),
        Pixel(2, 1, 0xff0000),
        Pixel(1, 2, 0xc80000),
        Pixel(2, 2, 0xcd0000),
        Pixel(13, 8, 0xc80000),
        Pixel(14, 8, 0xff0000),
        Pixel(15, 8, 0xc80000),
        Pixel(13, 9, 0xff0000),
        Pixel(14, 9, 0xff0000),
        Pixel(15, 9, 0xff0000),
        Pixel(13, 10, 0xff0000),
        Pixel(14, 10, 0xc80000),
        Pixel(15, 10, 0xff0000),
        Pixel(7, 17, 0xff0000),
        Pixel(8, 17, 0xff0000),
        Pixel(5, 18, 0xff0000),
        Pixel(6, 18, 0xff0000),
        Pixel(7, 18, 0xff0000)
      )

      clusterizer.clusters.size mustBe expectedPixels.size

      /**
        * Utility to check if expected pixels match computed pixels.
        *
        * @param expected expected pixels
        * @param actual actual pixels
        * @return true if expected pixels are all in the actual list of pixels.
        */
      def checkPixels(expected: List[Pixel], actual: ListBuffer[Pixel]
                     ): Boolean =
        expected match {
          case Nil => true
          case pixel::tail => {
            actual must contain (pixel)
            checkPixels(tail, actual)
          }
        }

      checkPixels(expectedPixels, clusterizer.clusters.map(_.pixels.head))
    }

    "clusterize correctly the test image" in {
      val expectedClusters = ListBuffer[Cluster](
        Cluster(List[Pixel](
          Pixel(1, 1, 0xff0000),
          Pixel(2, 1, 0xff0000),
          Pixel(1, 2, 0xc80000),
          Pixel(2, 2, 0xcd0000)
        )),
        Cluster(List[Pixel](
          Pixel(13, 8, 0xc80000),
          Pixel(14, 8, 0xff0000),
          Pixel(15, 8, 0xc80000),
          Pixel(13, 9, 0xff0000),
          Pixel(14, 9, 0xff0000),
          Pixel(15, 9, 0xff0000),
          Pixel(13, 10, 0xff0000),
          Pixel(14, 10, 0xc80000),
          Pixel(15, 10, 0xff0000)
        )),
        Cluster(List[Pixel](
          Pixel(7, 17, 0xff0000),
          Pixel(8, 17, 0xff0000),
          Pixel(5, 18, 0xff0000),
          Pixel(6, 18, 0xff0000),
          Pixel(7, 18, 0xff0000)
        ))
      )

      val clusters = clusterizer.clusterize()

      clusters.size mustBe expectedClusters.size

      /**
        * Check expected clusters are matching computed clusters
        *
        * @param expected expected clusters
        * @param actual computed clusters
        * @return true if the expected clusters are contained in the computed
        *         clusters.
        */
      def checkClusters(expected: ListBuffer[Cluster],
                        actual: ListBuffer[Cluster]
                       ): Boolean = {
        if (expected.isEmpty)
          return true

        actual must contain (expected.head)
        checkClusters(expected.tail, actual)
      }
      checkClusters(expectedClusters, clusters)
    }

  }

}
