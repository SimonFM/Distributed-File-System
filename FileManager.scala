import java.io.{FileOutputStream, PrintWriter, File}
import scala.io.Source

class FileManager {
  var filePaths : List[String]  = List()
  var files : List[File]  = List()
  var maxSize = 100
  var currentFileAccess : List[String]  = List()

  // writes a string to a file
  def writeToFile(fileName: String, toBeAdded:String): Boolean = {
      // If the current File is being written to.
      // cant access it.
      if(!currentFileAccess.contains(fileName)){


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

          if(!filePaths.contains(fileName)) filePaths = fileName :: filePaths
          if(!files.contains(file)) files = file :: files
          return true
        }
        else println("File System is full")
      }
      else println("Sorry File is being written to")

      // send back a failure
      false
  }

  // Checks if a file is currently being written to or not.
  def isFileBeingWrittenTo(fileName : String): Boolean ={
    currentFileAccess.contains(fileName)
  }

    // returns the contents of a file as a string.
  def getFileContents(fileName: String): String ={
      val file = new File(fileName)
      var result = ""
      if (!file.exists()) {
        println("Sorry '" + fileName + "' could not be found")
        return null
      }
      for (line <- Source.fromFile(fileName)) result = result + line
      result
  }

  // Returns the actual file and not the contents
  def getFile(fileName : String): File ={
    val file = new File(fileName)
    if(!file.exists()){
      currentFileAccess = fileName :: currentFileAccess
      file
    }
    else if( file.exists() && !currentFileAccess.contains(fileName) ){
      currentFileAccess = fileName :: currentFileAccess
      file
    }
    else null


  }

  // updates the content in a file
  def updateFile(fileName : String, newFile: Array[Byte]): Unit ={
    val oldFile = new File(fileName)

    val fileStream = new FileOutputStream(oldFile, false)
    fileStream.write(newFile)
    fileStream.close()
  }

  // releases that file from the list of currently accessed
  def releaseFile(fileName : String): Unit ={
    var newList : List[String] = List()
    for(file <- currentFileAccess){
      if(file != fileName) newList = file :: newList
    }
    currentFileAccess = newList
  }

  // Checks to see if a file is contained on a server
  def containsFile(fileName: String) : Boolean ={
     filePaths.contains(fileName)
  }

// performs LS command for the current FM
  def lsCommand(): List[String] ={
    for(f <- filePaths){
      println("Inside FM"+f)
    }
     filePaths
  }
}
