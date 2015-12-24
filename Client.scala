import java.io._
import java.net.Socket

import scala.io.Source

object Client {
  //Constants
  val WRITE = 0
  val READ = 1
  val DELETE = 2
  val EXIT = 9
  val HOSTNAME = "localhost"
  val PORT = 8000

  // Member data
  var socket = new Socket(HOSTNAME, PORT)
  var inStream  = new InputStreamReader(socket.getInputStream)
  var outputStream = socket.getOutputStream()
  var inVal = new BufferedReader(inStream)
  var out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8")))
  var clientName = ""
  val CACHE = new Cache


  // Writes to a file existing on a clients machine to the file system
  def writeFile(fileName : String): Unit ={
    val path = clientName + "/" + fileName
    val file = new File(path)
    if(!file.exists()){
      println("Sorry '" + path + "' could not be found")
    }
    else{
      val fileInput = new FileInputStream(file)
      val contents = new Array[Byte](file.length().toInt)

      fileInput.read(contents)
     // println(contents)
      println(path)
      //
      val lines = Source.fromFile(path).getLines().toList
      out.println("WRITE_FILE:")
      out.println("FILE_NAME:--"+fileName)
      for(l <- lines){
        out.println("CONTENTS:--" + l)
      }
      out.println("END;")
      out.flush() // send the request to the server



      // Wait for response
      var response = inVal.readLine()
      if(response.contains("SAVED:")){
        inVal.readLine() // END;
        println("Saved "+fileName)
        releaseFile(fileName)
      }
      else{
        println("An Error occured with "+fileName)
      }
    }
  }

  // Reads a desired File from the server
  def readFile(fileName : String): Unit ={

    if(!CACHE.isFileInCache(fileName)){
      // Sent the request to write
      out.println("GET_FILE:")
      out.println("FILE_NAME:--" + fileName)
      out.println("END;")
      out.flush() // send the request to the server

      // Wait for response
      var response = inVal.readLine()
      if(response.contains("FILE_CONTENTS:")){
        // println(response)
        var contents = List[String]()
        var response = ""
        var temp = ""
        while(response != "END;"){
          response = inVal.readLine()
          if(response != "END;" && response != "CONTENTS:--"){
            temp = response.split("--")(1)
            println(temp)
            contents = contents ++ List(temp)
          }
          else if( response == "CONTENTS:--")
            contents = contents ++ List("")

        }
       println("Got the file "+fileName)

        val file = new File(clientName + "/" + "cache" + "/" + fileName)

        val writer = new PrintWriter(file)
        for (l <- contents) {
          println("Contents: "+l)
          if("" != l){
            writer.write(l+"\n")
            writer.flush()
          }

        }
        writer.close()
        println("Wrote to the file")
        CACHE.addToCache(file)

      }
      else{
        println("An Error occurred with "+fileName)
      }
    }
    // it is in the cache, we need to ask the server if its the
    // latest version, if its not, then we need to update our copy
    else{
      // Ask for latest version
      out.println("GET_FILE_TIME:")
      out.println("TIME:--" + fileName)
      out.println("END;")
      out.flush() // send the request to the server

      // Get the latest time
      if(inVal.readLine() != "GET_FILE_TIME:") {
        val time = inVal.readLine().split("--")(1)
        println("Last Modified: " + time)
        inVal.readLine() // END;
        //Compare and update depending on the result

        val file = new File(clientName + "/" + "cache" + "/" + fileName)
        if (file.lastModified() != time.toLong) {
          // Sent the request to write
          out.println("GET_FILE:")
          out.println("FILE_NAME:--" + fileName)
          out.println("END;")
          out.flush() // send the request to the server

          // Wait for response
          var response = inVal.readLine()
          if (response.contains("FILE_CONTENTS:")) {
            // println(response)
            val contenstsMessage = inVal.readLine().split("--")(1)
            inVal.readLine() // END;
            println("Got the file " + fileName)

            val file = new File(clientName + "/" + "cache" + "/" + fileName)

            val bytesToBeWritten = contenstsMessage.getBytes
            val fStream = new FileOutputStream(clientName + "/" + "cache" + "/" + fileName)
            fStream.write(bytesToBeWritten)
            fStream.close()
            println("Wrote to the file")
            CACHE.addToCache(file)
          }
          else{
            inVal.readLine() // END;
            println("No Such File")
          }
        }

      }
      else println("Their the same")
    }
  }

  // Deletes a desired File from the server
  def deleteFile(fileName : String): Unit ={
    out.println("DELETE_FILE:")
    out.println("FILE_NAME:--"+fileName)
    out.println("END;")
  }

  // Tells the server to release a lock on a file
  def releaseFile(fileName : String): Unit ={
    out.println("RELEASE:")
    out.println("FILE_NAME:--"+fileName)
    out.println("END;")
  }

  // Makes the folder for the user with the desired name
  def makeDirectory(folderName : String): Unit ={
    val theDir = new File(folderName);
    if(!theDir.exists()){
      try {
        theDir.mkdirs()
      }catch{
        case secE: SecurityException  => println("Sorry you're not allowed to make folders")
      }
      println("Made a new Directory")
    }
  }

  // Disconnects the user from the server
  def disconnect(): Unit ={
    out.close()
    inVal.close()
    //CACHE.clear()
    socket.close()
  }

  def connect(): Unit ={
    socket = new Socket(HOSTNAME, PORT)
    inStream  = new InputStreamReader(socket.getInputStream)
    outputStream = socket.getOutputStream()
    inVal = new BufferedReader(inStream)
    out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8")))
  }

  // Makes a folder where all the client's reads are stored.
  def makeCache(): Unit ={
    makeDirectory(clientName + "/" + "cache")
    CACHE.folder = clientName + "/" + "cache"
  }

  def main(args: Array[String]): Unit ={
    clientName = readLine("Please enter your username: ")
    makeDirectory(clientName)
    makeCache()
    var choice = 0
    do{
      println("(0) - Write a file")
      println("(1) - Read a file")
      println("(2) - Delete a file")
      println("(9) - Exit")
      choice = readLine("Please Enter a choice (by number): ").toInt

      // do the choice
      choice match{
        case WRITE =>
          connect()
          val fileName = readLine("Please Enter the file name: ")
          writeFile(fileName)
          disconnect()
        case READ =>
          connect()
          val fileName = readLine("Please Enter the file name: ")
          readFile(fileName)
          disconnect()
        case DELETE =>
          connect()
          val fileName = readLine("Please Enter the file name: ")
          deleteFile(fileName)
          disconnect()
      }
    }while(choice != EXIT)
  }

}
