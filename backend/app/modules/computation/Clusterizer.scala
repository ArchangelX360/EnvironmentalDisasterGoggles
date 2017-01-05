package modules.computation

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer


/**
  * Utility class generating a cluster of points from an Image.
  *
  * This class reads an RGB image, gets channels to clusterize (currently only
  * the red channel) and, using a threshold value, creates clusters of points.
  *
  * As an example, suppose we have the following red channel :
  *     255 234 180 120 97  123 12
  *     201 243 177 12  18  129 60
  *     120 169 100 243 230 112 190
  *     12  54  91  251 200 100 0
  *     0   15  71  161 182 190 100
  *
  * This class will get all pixels matching a specific threshold value (here,
  * 200). The representation of the selected pixels would be:
  *     1 1 0 0 0 0 0
  *     1 1 0 0 0 0 0
  *     0 0 0 1 1 0 0
  *     0 0 0 1 1 0 0
  *     0 0 0 0 0 0 0
  *
  * On this matrix we can see two different groups of pixels. This class will
  * generate two cluster, one in (x: 1/2, y: 1/2) and the other one in (x: 9/2,
  * y: 9/2), both with a radius of the maximum distance from the barycentre,
  * i.e. sqrt(1/2) ~= 0.7.
  */
class Clusterizer(imageFile: File) {

  /** Load the image */
  val image: BufferedImage = ImageIO.read(imageFile)

  /** Image width. */
  lazy val width: Int = image.getWidth

  /** Image height. */
  lazy val height: Int = image.getHeight

  /**
    * Threshold value used to transform the image from a standard matrix to a
    * matrix of truly values. Must be contained between 0 and 255.
    *
    * TODO(funkysayu): Modifying this variable is not supported at the moment
    */
  var threshold = 200

  /**
    * Checks if the pixel is matching the threshold
    *
    * @param pixel Pixel to check
    * @return True if the pixel is above the threshold limit
    */
  def matchTreshold(pixel: Pixel): Boolean = pixel.red >= threshold

  /**
    * Generates clusters from the image.
    *
    * Each iteration merges one or multiple clusters with minimal distance
    * between them.
    *
    * TODO(funkysayu) Adapt the code to move to a List instead of ListBuffer
    *
    * @return The list of initial clusters, extracted from the image.
    */
  private def generateClusters(): ListBuffer[Cluster] =
    (0 until this.height * this.width).foldLeft(List[Cluster]())(
      (result, current) => {
        val x = current / this.height
        val y = current % this.height
        val pixel = Pixel(x, y, this.image.getRGB(x, y))

        if (matchTreshold(pixel))
          Cluster(List[Pixel](pixel)) :: result
        else
          result
      }).to[ListBuffer]

  var clusters: ListBuffer[Cluster] = generateClusters()

  /**
    * Merges the clusters based on a list of merge operations.
    *
    * @param mergeOperations list of operations to apply on the cluster. Each
    *                        operation consists of two clusters to merge.
    */
  private def mergeClusters(mergeOperations: List[(Cluster, Cluster)]): Unit =
    mergeOperations match {
      case Nil => Unit
      case (first: Cluster, second: Cluster)::tail => {
        val indexFirst = this.clusters.indexOf(first)
        val indexSecond = this.clusters.indexOf(second)

        // Ensure clusters exists. They might be removed due to precedent
        // operations.
        if (indexFirst != -1 && indexSecond != -1) {
          val newCluster = this.clusters(indexFirst).merge(
            this.clusters(indexSecond))
          this.clusters.remove(indexSecond)
        }

        mergeClusters(tail)
      }
    }

  /**
    * Creates the list of merge operations to do.
    *
    * Merge operations are composed to two clusters to merge. This is based
    * on the lowest distance within two clusters.
    *
    * @return the list of merge operations to do.
    */
  private def createMergeOperations(): List[(Cluster, Cluster)] = {

    /**
      * Function utility calculating rounded up distance between two clusters
      *
      * @param mergeOperation tuple of two clusters on which we will evaluate
      *                       distance
      * @return rounded up distance between the both clusters
      */
    def ceiledDistance(mergeOperation: (Cluster, Cluster)): Double =
      Math.ceil(mergeOperation._1.distance(mergeOperation._2))

    // Create combination iterator and convert it to merge operation
    val combined = clusters.combinations(2).map(element =>
      (element.head, element(1)))

    combined.foldLeft(List(combined.next()))((result, current) => {
      // Note: distances are cached, so even if it appears we are calculating
      // them over and over, we don't.
      val headDistance = ceiledDistance(result.head)
      val currentDistance = ceiledDistance(current)

      if (headDistance > currentDistance)
        List[(Cluster, Cluster)](current)
      else if (headDistance == currentDistance)
        current :: result
      else
        result
    })
  }

  /**
    * Checks if the previous cluster is "better" than the current one.
    *
    * By "better", we mean more relevant than the previous one. For example,
    * if we take the example shown in this class documentation, we should have
    * 2 clusters containing 4 points each. This function ensure clusterization
    * is optimal.
    *
    * @param previous List of previous clusters before the new merging
    *                 operations
    * @return a boolean indicating if the previous list of clusters is better
    *         than the current one
    */
  private def isBetterClusterization(previous: ListBuffer[Cluster]): Boolean = {
    // TODO(funkysayu) do some real comparisons here.
    this.clusters.size <= 3
  }

  /**
    * Create clusters of elements.
    */
  final def clusterize(): ListBuffer[Cluster] = {
    val previous = this.clusters
    val mergeOperations = createMergeOperations()
    mergeClusters(mergeOperations)

    if (!isBetterClusterization(previous))
      return clusterize()

    this.clusters
  }

}
