import java.io.{File, FileOutputStream}

import scala.io.Source

class FileManager {
  var filePaths : List[String]  = List()
  var files : List[File]  = List()
  var maxSize = 100
  var currentFileAccess : List[String]  = List()

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
    if( file.exists() && !currentFileAccess.contains(fileName) ){
      currentFileAccess = fileName :: currentFileAccess
      filePaths = fileName :: filePaths
      file
    }
    else if(!file.exists()){
      currentFileAccess = fileName :: currentFileAccess
      filePaths = fileName :: filePaths
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
     return filePaths.contains(fileName)
  }

  // performs LS command for the current FM
  def lsCommand(): List[String] ={
    return filePaths
  }

  def makeDirectory(folderName : String): Unit ={
    val theDir = new File(folderName);
    if(!theDir.exists()){
      try theDir.mkdirs()
      catch{
        case secE: SecurityException  =>
          println("Sorry you're not allowed to Make Directories")
      }
      println("Made a new Directory")
    }
  }
}
