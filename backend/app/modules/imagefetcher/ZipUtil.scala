package modules.imagefetcher

import java.io.{File, FileInputStream, FileOutputStream}
import java.util.zip.ZipInputStream

import com.google.common.io.ByteStreams

object ZipUtil {

  /**
    * Extract all file with the specified extension from a zip file
    *
    * @param file         File object representing the zip file
    * @param extension    A string which is matched against file in the zip to determine which one to extract
    * @param outputFolder Folder in which the file will be extracted
    * @return An optional result containing a path to the first file extracted
    */
  def extractZip(file: File, extension: String = ".png", outputFolder: String = "./Downloaded"): Option[File] = {

    val zipStream = new ZipInputStream(new FileInputStream(file))

    while (zipStream.available() != 0) {

      val zipEntry = zipStream.getNextEntry
      val filename = zipEntry.getName

      if (filename.endsWith(extension)) {

        // Extract the image from the archive
        val outputFile = new File(outputFolder + File.separator + filename)
        val outputStream: FileOutputStream = new FileOutputStream(outputFile)

        ByteStreams.copy(zipStream, outputStream)
        outputStream.close()

        return Some(outputFile)
      }

    }

    None

  }

}
