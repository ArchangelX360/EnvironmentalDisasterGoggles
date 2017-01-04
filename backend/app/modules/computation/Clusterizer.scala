package modules.computation

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer


/**
  * Utility class generating a cluster of points from an Image.
  *
  * This class read an RGB image, get channels to clusterize (e.g. Red only or
  * average between multiple channels) and, using a treshold value, create
  * clusters of points.
  *
  * As an example, suppose we have the following red channel :
  *     255 234 180 120 97  123 12
  *     201 243 177 12  18  129 60
  *     120 169 100 243 230 112 190
  *     12  54  91  251 200 100 0
  *     0   15  71  161 182 190 100
  *
  * Using the a treshold value of 200, we will set to one all points above the
  * treshold to 1 and the other ones to 0:
  *     1 1 0 0 0 0 0
  *     1 1 0 0 0 0 0
  *     0 0 0 1 1 0 0
  *     0 0 0 1 1 0 0
  *     0 0 0 0 0 0 0
  *
  * On this matrice we can see two different groups of pixels. This class will
  * generate two cluster, one in (x: 1/2, y: 1/2) and the other one in (x: 9/2,
  * y: 9/2), both with a radius of the maximum distance from the baricentre,
  * i.e. sqrt(1/2) ~= 0.7.
  */
class Clusterizer(imageFile: File) {

  /**
    * Lazy load of the image pixels
    */
  val image: BufferedImage = ImageIO.read(imageFile)

  /**
    * Image width.
    */
  lazy val width: Int = image.getWidth

  /**
    * Image height.
    */
  lazy val height: Int = image.getHeight

  /**
    * Treshold value used to transform the image from a standard matrix to a
    * matrix of truly values. Must be contained between 0 and 255.
    */
  var threshold = 200

  /**
    * Check if the pixel is matching the threshold
    *
    * @param pixel Pixel to check
    * @return True if the pixel is above the threshold limit
    */
  def matchTreshold(pixel: Pixel): Boolean = pixel.red >= threshold

  /**
    * Cluster generator.
    *
    * Each iteration merges one or multiple clusters with minimal distance
    * between them.
    *
    * @param current Current pixel to analyze
    */
  @tailrec
  private def generateClusters(current: Int = 0,
                               accumulator: ListBuffer[Cluster]
                                          = ListBuffer[Cluster]()
                              ): ListBuffer[Cluster] = {
    if (current >= this.height * this.width)
      return accumulator

    val x = current / this.height
    val y = current % this.height
    val pixel = Pixel(x, y, this.image.getRGB(x, y))

    if (matchTreshold(pixel))
      accumulator.append(Cluster(List[Pixel](pixel)))

    generateClusters(current + 1, accumulator)
  }

  var clusters: ListBuffer[Cluster] = generateClusters()

  /**
    * Merge the clusters based on a list of merge operations.
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
    * Create the list of merge operations to do.
    *
    * Merge operations are composed to two clusters to merge. This is based
    * on the lowest distance within two clusters.
    *
    * @return the list of merge operations to do.
    */
  private def createMergeOperations(): List[(Cluster, Cluster)] = {
    var minimumDistance = Double.PositiveInfinity

    /**
      * Modify the list of merge operations depending of two clusters.
      *
      * Overwrite the current accumulator if the two clusters have a lower
      * distance ; append the two clusters if the distance is equal to the other
      * ones
      *
      * @param first first cluster to compare
      * @param second second cluster to compare
      * @param accumulator list of merge operations to do
      * @return the (eventually modified) list of merge operations.
      */
    def generateAccumulator(first: Cluster,
                            second: Cluster,
                            accumulator: List[(Cluster, Cluster)]
                           ): List[(Cluster, Cluster)] = {
      val distance = first.distance(second).round

      if (distance < minimumDistance) {
        minimumDistance = distance
        List[(Cluster, Cluster)]((first, second))
      } else if (distance == minimumDistance)
        (first, second) :: accumulator
      else
        accumulator
    }

    /**
      * Finds the closest clusters.
      *
      * This function evaluate distances from a cluster to other clusters, to
      * select the closest ones. Rounded distances must be lower than the
      * minimum distance defined in the above scope.
      *
      * @param cluster current cluster to evaluate.
      * @param remainingClusters list of remaining clusters to evaluate.
      * @param accumulator list of closest clusters, returned at the end.
      * @return The list of closest clusters.
      */
    @tailrec
    def closestCluster(cluster: Cluster,
                       remainingClusters: ListBuffer[Cluster],
                       accumulator: List[(Cluster, Cluster)]
                      ): List[(Cluster, Cluster)] = {
      if (remainingClusters.isEmpty)
        return accumulator
      closestCluster(cluster, remainingClusters.tail, generateAccumulator(
        cluster, remainingClusters.head, accumulator))
    }

    /**
      * Generates the list of closest clusters in the current cluster list.
      *
      * @param clusters list of clusters to evaluate.
      * @param accumulator list of closest clusters, returned at the end.
      * @return The list of closest clusters.
      */
    @tailrec
    def generator(clusters: ListBuffer[Cluster],
                  accumulator: List[(Cluster, Cluster)]
                 ): List[(Cluster, Cluster)] = {
      if (clusters.isEmpty)
        return accumulator

      generator(clusters.tail, closestCluster(clusters.head, clusters.tail,
        accumulator))
    }

    generator(this.clusters, List[(Cluster, Cluster)]())
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
