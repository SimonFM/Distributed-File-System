import java.io.{FileOutputStream, File}

class Cache {
  var contents : List[File] = List()
  val maxSize = 10
  val currentSize = 0
  var folder = ""
  // adds to the cache
  def addToCache(file: File): Unit ={

    if(currentSize == maxSize)  {
      contents = contents.updated(0, file)
      println("Popped from cache")
    }
    else if(contents.contains(file)){
      println("That file is already in the cache")
    }
    else{
      contents = file :: contents
      println("Added file to the cache")
    }
  }
  def getFile( fileName: String): File ={
    for(file <- contents){
      if(file.getName == fileName)return file
    }
    return null
  }

  def updateFile(file: File): Unit ={

  }

  def isFileInCache(nFile: String): Boolean ={
    for(file <- contents){
      if(file.getName == nFile){
        println("Found the file " + nFile + " in the cache")
        return true
      }
    }
    println("Could not find the file " + nFile + " in the cache")
    return false
  }

  def writeToFile(nFile : String, newContents : String): Unit ={
    for(file <- contents){
      if(file.getName == nFile){
        val bytesToBeWritten = newContents.getBytes
        val fStream = new FileOutputStream(folder+nFile)
        fStream.write(bytesToBeWritten)
        fStream.flush()
        fStream.close()
        println("Wrote To " +folder+nFile + " in the cache")
        println("Wrote " + new String(bytesToBeWritten) + " in the cache")
      }
    }
  }
  // clears the cache's contents
  def clear(): Unit ={
    contents = List()
    println("Cache Cleared")
  }
}
