package modules.imagefetcher

import java.io.{File, FileInputStream, FileOutputStream}
import java.util.zip.ZipInputStream

object ZipUtil {

  /**
    * Extract all file with the specified extension from a zip file
    * @param file File object representing the zip file
    * @param extension A string which is matched against file in the zip to determine which one to extract
    * @param outputFolder Folder in which the file will be extracted
    * @return An optional result containing a path to the first file extracted
    */
  def extractZip(file: File, extension: String = ".png", outputFolder: String = "./Downloaded"): Option[File] = {

    val zipStream = new ZipInputStream(new FileInputStream(file))

    while(zipStream.available() != 0) {

      val zipEntry = zipStream.getNextEntry

      val filename = zipEntry.getName

      if (filename.endsWith(extension)) {

        val outputFile = new File(outputFolder + File.separator + filename)

        // Extract the image from the archive
        val outputStream: FileOutputStream = new FileOutputStream(outputFile)
        var size: Int = 0
        val buffer = new Array[Byte](1024)

        size = zipStream.read(buffer)

        try {

          while (size != 0) {
            outputStream.write(buffer, 0, size)
            size = zipStream.read(buffer)
          }
        } catch {
          case e: Throwable => println(e)
        }

        outputStream.close()
        return Some(outputFile)
      }

    }

    None

  }
}
