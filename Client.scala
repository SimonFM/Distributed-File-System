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
  var directorySocket = new Socket(HOSTNAME, PORT)
  var inStream  = new InputStreamReader(directorySocket.getInputStream)
  var outputStream = directorySocket.getOutputStream()
  var directoryIn = new BufferedReader(inStream)
  var directoryOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8")))
  var clientName = ""
  val CACHE = new Cache

  // SEARCH:
  // FILENAME:--test.txt
  // END;
  def whereIs(fileName:String): String ={
    directoryOut.println("SEARCH:")
    directoryOut.println("FILEPATH:--"+fileName)
    directoryOut.println("END;")
    directoryOut.flush() // send the request to the server
    if(directoryIn.readLine() == "SEARCH:"){
      val ip = directoryIn.readLine().split("--")(1) // IP
      val port = directoryIn.readLine().split("--")(1) // PORT
      directoryIn.readLine() // end
      println("We found it on " + ip + "--" + port)
      return ip + "--" + port
    }
    else return "NOPE"
  }

  // Writes to a file existing on a clients machine to the file system
  def writeFile(fileName : String): Unit ={
    val path = clientName + "/" + "cache"+ "/" + fileName
    val file = new File(path)
    if(!file.exists()) println("Sorry '" + path + "' could not be found")
    else{
      val result = whereIs(fileName)
      if(result != "NOPE"){
        val IP = result.split("--")(0)
        val PORT = result.split("--")(1)

        // connect to the designated node
        val nodeSocket = new Socket(IP, PORT.toInt)
        val nodeOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(nodeSocket.getOutputStream, "UTF-8")))
        val nodeInStream = new InputStreamReader(nodeSocket.getInputStream)
        lazy val nodeInVal = new BufferedReader(nodeInStream)
        val fileInput = new FileInputStream(file)
        val contents = new Array[Byte](file.length().toInt)

        fileInput.read(contents)
        println("Sending Request to write")
        //
        val lines = Source.fromFile(path).getLines().toList
        nodeOut.println("WRITE_FILE:")
        nodeOut.println("FILE_NAME:--"+fileName)
        for(l <- lines) nodeOut.println("CONTENTS:--" + l)
        nodeOut.println("END;")
        nodeOut.flush() // send the request to the server
        println("Waiting for write ACK")
        // Wait for response
        var response = nodeInVal.readLine()
        println(response)
        if(response.contains("SAVED:")){
          nodeInVal.readLine() // END;
          println("Saved "+fileName)
          //releaseFile(fileName)
        }
        else{
          println("An Error occured with "+fileName)
        }
      }
      else println("That file could not be found")
    }

  }

  // Reads a desired File from the server
  def readFile(fileName : String): Unit ={
    val result = whereIs(fileName)
    if(result != "NOPE"){
      if(!CACHE.isFileInCache(fileName)){
        val IP = result.split("--")(0)
        val PORT = result.split("--")(1)
        val nodeSocket = new Socket(IP, PORT.toInt)
        val nodeOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(nodeSocket.getOutputStream, "UTF-8")))
        val nodeInStream = new InputStreamReader(nodeSocket.getInputStream)
        lazy val nodeInVal = new BufferedReader(nodeInStream)
        // Sent the request to write
        nodeOut.println("GET_FILE:")
        nodeOut.println("FILE_NAME:--" + fileName)
        nodeOut.println("END;")
        nodeOut.flush() // send the request to the server

        // Wait for response
        var response = nodeInVal.readLine()
        println(response)
        if(response.contains("FILE_CONTENTS:")){
          // println(response)
          var contents = List[String]()
          response = ""
          var temp = ""
          while(response != "END;"){
            response = nodeInVal.readLine()
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
        val IP = result.split("--")(0)
        val PORT = result.split("--")(1)
        val nodeSocket = new Socket(IP, PORT.toInt)
        val nodeOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(nodeSocket.getOutputStream, "UTF-8")))
        val nodeInStream = new InputStreamReader(nodeSocket.getInputStream)
        lazy val nodeInVal = new BufferedReader(nodeInStream)
        // Ask for latest version
        nodeOut.println("GET_FILE_TIME:")
        nodeOut.println("TIME:--" + fileName)
        nodeOut.println("END;")
        nodeOut.flush() // send the request to the server

        // Get the latest time
        if(nodeInVal.readLine() != "GET_FILE_TIME:") {
          val time = nodeInVal.readLine().split("--")(1)
          println("Last Modified: " + time)
          nodeInVal.readLine() // END;
          //Compare and update depending on the result

          val file = new File(clientName + "/" + "cache" + "/" + fileName)
          if (file.lastModified() != time.toLong) {
            // Sent the request to write
            nodeOut.println("GET_FILE:")
            nodeOut.println("FILE_NAME:--" + fileName)
            nodeOut.println("END;")
            nodeOut.flush() // send the request to the server

            // Wait for response
            var response = nodeInVal.readLine()
            if (response.contains("FILE_CONTENTS:")) {
              // println(response)
              val contenstsMessage = nodeInVal.readLine().split("--")(1)
              nodeInVal.readLine() // END;
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
              nodeInVal.readLine() // END;
              println("No Such File")
            }
          }

        }
        else println("They're the same")
      }
    }
    else println("SERVER DOES NOT HAVE A FILE CALLED: "+fileName)

  }

  // Deletes a desired File from the server
  def deleteFile(fileName : String): Unit ={
    directoryOut.println("DELETE_FILE:")
    directoryOut.println("FILE_NAME:--"+fileName)
    directoryOut.println("END;")
  }

  // Tells the server to release a lock on a file
  def releaseFile(fileName : String): Unit ={
    directoryOut.println("RELEASE:")
    directoryOut.println("FILE_NAME:--"+fileName)
    directoryOut.println("END;")
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
    directoryOut.close()
    directoryIn.close()
    //CACHE.clear()
    directorySocket.close()
  }

  def connect(): Unit ={
    directorySocket = new Socket(HOSTNAME, PORT)
    inStream  = new InputStreamReader(directorySocket.getInputStream)
    outputStream = directorySocket.getOutputStream()
    directoryIn = new BufferedReader(inStream)
    directoryOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8")))
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
