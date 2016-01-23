
class FileMap {
  var fileToPortReference : Map[String, Int] = Map()
  val defaultPort = 8080
  val maxPort = 8082
  var currentPort = 8080

  // add to the map.
  def addToMap(file : String): Unit ={
    if(!fileToPortReference.contains(file)){
      fileToPortReference = Map(file -> currentPort) ++ fileToPortReference
      updatePortPointer()
    }
    else println("We already have a file with that name")
  }

  // delete the references
  def removeFromMap(file : String): Unit ={
    var newMap : Map[String, Int] = Map()
    for(value <- fileToPortReference){
      if(value._1 != file) // if the keys aren't the same
        newMap = Map(value._1 -> value._2) ++ newMap
    }
    fileToPortReference = newMap
  }

  def updatePortPointer(): Unit ={
    if(currentPort >= maxPort ) currentPort = defaultPort
    else currentPort = currentPort + 1
  }

  // checks if the file exists
  def fileExists(file : String): Boolean ={
    return fileToPortReference.contains(file)
  }

  // returns the associated port with the file
  def getPort(file : String): Int ={
    if(fileToPortReference.contains(file)){
      return fileToPortReference(file)
    }
    return -99
  }
}
