package modules.computation

import org.scalatestplus.play._

/**
  * Tests the modules.computation.Pixel class
  */
class ClusterTest extends PlaySpec {

  "A Cluster" must {
    "computes its size correctly" in {
      var cluster = Cluster(List(Pixel(0, 0, 0), Pixel(1, 1, 1)))
      cluster.size mustBe 2

      cluster = Cluster(List(Pixel(1, 1, 1)))
      cluster.size mustBe 1
    }
    "computes its baricentre correctly" in {
      var cluster = Cluster(List(Pixel(0, 0, 0), Pixel(1, 1, 0)))
      cluster.x mustBe 0.5
      cluster.y mustBe 0.5

      cluster = Cluster(List(Pixel(0, 0, 0), Pixel(2, 2, 0)))
      cluster.x mustBe 1
      cluster.y mustBe 1

      cluster = Cluster(List(Pixel(0, 0, 0), Pixel(2, 0, 0)))
      cluster.x mustBe 1
      cluster.y mustBe 0

      cluster = Cluster(List(Pixel(0, 0, 0), Pixel(0, 2, 0)))
      cluster.x mustBe 0
      cluster.y mustBe 1

      cluster = Cluster(List(Pixel(2, 2, 0)))
      cluster.x mustBe 2
      cluster.y mustBe 2
    }
    "evaluates its distance to another cluster correctly" in {
      val first = Cluster(List(Pixel(0, 0, 0), Pixel(2, 2, 0)))
      val second = Cluster(List(Pixel(3, 4, 0), Pixel(5, 6, 0)))

      first.distance(second) mustBe 5
      // Same test, to see if cache generates issues.
      first.distance(second) mustBe 5
      // TODO(funkysayu): test already computed distances are cached
    }
    "merges another cluster in place" in {
      val initial = List(Pixel(0, 0, 0), Pixel(2, 2, 0))
      val first = Cluster(initial)
      val second = Cluster(List(Pixel(3, 4, 0), Pixel(5, 6, 0)))

      first.merge(second)
      first.size mustBe 4
      first.x mustBe 2.5
      first.y mustBe 3
      first.pixels mustBe (initial ::: second.pixels)

      // Ensure second is not modified
      second.size mustBe 2
      second.x mustBe 4
      second.y mustBe 5
    }
    "merges correctly another cluster with different size" in {
      val initial = List(Pixel(0, 0, 0))
      val first = Cluster(initial)
      val second = Cluster(List(Pixel(2, 4, 0), Pixel(4, 8, 0)))

      first.merge(second)
      first.size mustBe 3
      first.x mustBe 2
      first.y mustBe 4
      first.pixels mustBe (initial ::: second.pixels)
    }
  }

}
