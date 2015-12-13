import java.io.{PrintWriter, File}

import scala.io.Source

/**
 * Created by simon on 05/12/2015.
 */
class FileManager {
  var filePaths : List[String]  = List()

  def writeToFile(fileName: String, toBeAdded:String): Unit = {
    val file = new File(fileName)

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
