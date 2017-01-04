package modules.computation

/**
  * Class used to represent a pixel, giving its value and its position.
  */
case class Pixel(x: Integer, y: Integer, value: Integer) {

  /** Value of the red channel. */
  lazy val red: Integer = (value & 0xff0000) >> 16

  /** Value of the green channel. */
  lazy val green: Integer = (value & 0x00ff00) >> 8

  /** Value of the blue channel */
  lazy val blue: Integer = value & 0x0000ff

  /**
    * Computes the euclidean distance between two clusters
    *
    * @param other Another pixel to measure difference
    * @return The euclidean distance between self and the other pixel.
    */
  def distance(other: Pixel): Double = {
    Math.sqrt(Math.pow(x - other.x, 2) + Math.pow(y - other.y, 2))
  }

  /**
    * Check values specifically matching our needs.
    *
    * `value` field may contain more several unused information, such as alpha
    * channel or other. As we do not use them, only checks RGB channels and
    * pixel position.
    *
    * @param other Other element to check equality with.
    * @return true if the two pixels have the same position and same RGB values
    */
  override def equals(other: Any): Boolean =
    other match {
      case p: Pixel => (p.red == red && p.green == green && p.blue == blue
        && p.x == x && p.y == y)
      case _ => false
    }

}