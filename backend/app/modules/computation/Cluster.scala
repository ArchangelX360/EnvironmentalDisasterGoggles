package modules.computation

import scala.collection.mutable

/**
  * Represents a cluster of pixels.
  *
  * This is mostly a utility class to manipulate pixels.
  *
  * @param pixels Pixels composing the cluster.
  */
case class Cluster(var pixels: List[Pixel]) {

  /** Shortcut for cluster size */
  var size: Int = pixels.size.intValue()

  /** Cluster barycentre */
  var x: Double = pixels.map(_.x).foldLeft(0)(_ + _) / size.toFloat
  var y: Double = pixels.map(_.y).foldLeft(0)(_ + _) / size.toFloat

  /** Store computed distances to another cluster. */
  private val distances = new mutable.HashMap[Cluster, Double]()

  /**
    * Compute the distance to another cluster and save it in the cache.
    *
    * @param other Other cluster.
    * @return computed distance from this cluster to the other.
    */
  private def computeDistance(other: Cluster): Double = {
    val distance = Math.sqrt(Math.pow(this.x - other.x, 2) +
      Math.pow(this.y - other.y, 2))
    distances += other -> distance
    distance
  }

  /**
    * Distance from one cluster to another.
    *
    * @param other Other cluster.
    * @return distance from this cluster to the other, from cache or computed.
    */
  def distance(other: Cluster): Double = {
    distances.getOrElse(other, other.distances.getOrElse(this,
      computeDistance(other)))
  }

  /**
    * In-place merging of another cluster.
    *
    * @param other Other cluster to merge in this one.
    */
  def merge(other: Cluster): Unit = {
    val size = this.pixels.size + other.size

    // Compute the new barycentre
    this.x = (this.x * this.size + other.x * other.size) / size
    this.y = (this.y * this.size + other.y * other.size) / size
    this.size = size

    // Update the pixels list
    this.pixels = this.pixels ::: other.pixels
  }

  /**
    * Compare two clusters and ensure they are equals.
    *
    * @param obj element to compare
    * @return true if the two clusters contains the same pixels.
    */
  override def equals(obj: scala.Any): Boolean = obj match {
    case cluster: Cluster =>
      cluster.size == size && cluster.pixels.forall(pixels contains _)
    case _ => false
  }

}