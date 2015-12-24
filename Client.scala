import java.io._
import java.net.Socket

object Client {
  val host = "localhost"
  //val host = ""
  val port = 8000
  val socket = new Socket(host, port)
  val inStream  = new InputStreamReader(socket.getInputStream)
  val outputStream = socket.getOutputStream()
  lazy val inVal = new BufferedReader(inStream)
  val out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8")))
  var clientName = ""
  val cache = new Cache

  // Writes to a file existing on a clients machine to
  // the file system
  def writeFile(fileName : String): Unit ={

    val file = new File(clientName + "/" + fileName)
    if(!file.exists()){
      println("Sorry '" +clientName + "/" +fileName + "' could not be found")
    }
    else{
      val fileInput = new FileInputStream(file)
      val contents = new Array[Byte](file.length().toInt)
      fileInput.read(contents)

      // Sent the request to write
      out.println("WRITE_FILE:")
      out.println("FILE_NAME:--"+fileName)
      out.println("CONTENTS:--" + new String(contents))
      out.println("END;")
      out.flush() // send the request to the server

      // Wait for response
      var response = inVal.readLine()
      if(response.contains("SAVED:")){
        inVal.readLine() // END;
        println("Saved "+fileName)
      }
      else{
        println("An Error occured with "+fileName)
      }
    }
  }

  def readFile(fileName : String): Unit ={

    if(!cache.isFileInCache(fileName)){
      // Sent the request to write
      out.println("GET_FILE:")
      out.println("FILE_NAME:--" + fileName)
      out.println("END;")
      out.flush() // send the request to the server

      // Wait for response
      var response = inVal.readLine()
      if(response.contains("FILE_CONTENTS:")){
        // println(response)
        val contenstsMessage = inVal.readLine().split("--")(1)
        inVal.readLine() // END;
        println("Got the file "+fileName)

        val file = new File(clientName + "/" + "cache" + "/" + fileName)

        val bytesToBeWritten = contenstsMessage.getBytes
        val fStream = new FileOutputStream(clientName + "/" + "cache" + "/" + fileName)
        fStream.write(bytesToBeWritten)
        fStream.close()
        println("Wrote to the file")
        cache.addToCache(file)

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
            cache.addToCache(file)
          }
        }

      }
      else println("Their the same")
    }
  }

  def deleteFile(fileName : String): Unit ={

  }

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

  def disconnect(): Unit ={
    out.close()
    inVal.close()
    cache.clear()
  }

  def makeCache(): Unit ={
    makeDirectory(clientName + "/" + "cache")
    cache.folder = clientName + "/" + "cache"
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
        case 0 =>
          val fileName = readLine("Please Enter the file name: ")
          writeFile(fileName)
        case 1 =>
          val fileName = readLine("Please Enter the file name: ")
          readFile(fileName)
        case 2 =>
          val fileName = readLine("Please Enter the file name: ")
          deleteFile(fileName)
      }
    }while(choice != 9)
  }

}
