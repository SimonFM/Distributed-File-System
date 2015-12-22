import java.io.{PrintWriter, File}

import scala.io.Source

/**
 * Created by simon on 05/12/2015.
 */
class FileManager {
  var filePaths : List[String]  = List()
  var maxSize = 100
  def writeToFile(fileName: String, toBeAdded:String): Unit = {
    if(maxSize >= filePaths.size){
      val file = new File(fileName)
      println("To Add "+fileName)
      if(!file.exists()) {
        file.createNewFile()
        println("Added "+fileName + " to file paths")
      }

      val writer = new PrintWriter(file)
      writer.write(toBeAdded)
      writer.close()

      if(!filePaths.contains(fileName))
        filePaths = fileName :: filePaths
    }
    else println("File System is full")

  }

  def getFileContents(fileName: String): String ={
    val file = new File(fileName)
    var result = ""
    if(!file.exists()) {
      println("Sorry '"+ fileName+"' could not be found")
      return null
    }
    for(line <- Source.fromFile(fileName)) result = result + line

    return result
  }

  def containsFile(fileName: String) : Boolean ={
    return filePaths.contains(fileName)
  }


  def lsCommand(): List[String] ={
    for(f <- filePaths){
      println("Inside FM"+f)
    }
    return filePaths
  }
}
