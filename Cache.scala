import java.io.File

class Cache {
  var contents : List[File] = List()
  val maxSize = 10
  val currentSize = 0

  // adds to the cache
  def addToCache(file: File): Unit ={
    if(currentSize == maxSize)  {
      contents = contents.updated(0, file)
      println("Popped from cache")
    }
    else if(contents.contains(file)){
      val index = contents.indexOf(file)
      contents = contents.updated(index, file)
      println("That file is already in the cache")
    }
    else{
      contents = file :: contents
      println("Added file to the cache")
    }
  }
  def getFile( fileName: String): File ={
    for(file <- contents){
      if(file.getName == fileName) file
    }
    null
  }

  def updateFile(file: File): Unit ={

  }

  def isFileInCache(file: File): Boolean ={
    contents.contains(file)
  }
  // clears the cache's contents
  def clear(): Unit ={
    contents = List()
    println("Cache Cleared")
  }
}
