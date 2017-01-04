package modules.computation

import org.scalatestplus.play._

/**
  * Tests the modules.computation.Pixel class
  */
class PixelTest extends PlaySpec {

  "A Pixel" must {
    "compute RGB values correctly" in {
      val pixel = Pixel(0, 0, 0xabcdef)

      pixel.red mustBe 0xab
      pixel.green mustBe 0xcd
      pixel.blue mustBe 0xef
    }
    "compute distance from another pixel correctly" in {
      val pixel = Pixel(2, 2, 0x0)

      val xMoved = Pixel(2, 0, 0x0)
      pixel.distance(xMoved) mustBe 2
      val yMoved = Pixel(0, 2, 0x0)
      pixel.distance(yMoved) mustBe 2
      val xyMoved = Pixel(5, 6, 0x0)
      pixel.distance(xyMoved) mustBe 5
    }
  }

}