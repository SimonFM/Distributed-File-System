/**
 * Created by Simon on 05/12/2015.
 * Based off my previous labs
 */
import java.io._
import java.net._
import java.util.concurrent.Executors

import scala.io.Source

object DirectoryServer {

  var connection = 0
  var id = 0
  val numberOfNodes = 3
  val HOST = "localhost"

  var portsToFile : Map[String, Int] = Map()

  val cache = new Cache
  val folder = "DirectorySever"

  cache.folder = folder+"DirectoryCache"
  /**
   * This is a simple server class to represent a multi threaded server.
   * It contains both a Server and a Worker class. The worker doing all the
   * work for the server.
   * @param portNumber - The port the server operates on.
   */
  class Server(portNumber: Int) extends Runnable {
    var NUMBER_OF_THREADS = 20
    // Maximum number of threads.
    val serverSocket = new ServerSocket(portNumber) // setting up the server

    println("Server running on port number: " + portNumber)
    // display to console
    val threadPool = Executors.newFixedThreadPool(NUMBER_OF_THREADS) // create the thread pool

    var sockets: List[Socket] = List()
    var noKill = true
    val fileMap = new FileMap

    /**
     * This is the run method of the server, it is needed as I have extended my server
     * to be Runnable, so I could have multiple servers should the need arise.
     * It creates a new socket for every new connection to the server.
     * It loops forever, as long as the server is not closed.
     */
    def run(): Unit = {
      try {

        while (!serverSocket.isClosed && noKill) {
          try {
            sockets = serverSocket.accept() :: sockets
            if (sockets.nonEmpty) {
              println("New Client requested to connect")
              threadPool.execute(new Worker(sockets.head)) // allocate a new Worker a Socket
              connection = connection + 1
            }
            else println("Empty socket list")
          } catch {
            case socketE: SocketException =>
              serverSocket.close()
              println("Sorry, the server isn't running")
          }
        }
      } finally {
        println("Thread Pool shutdown")
        threadPool.shutdown()
      }
    }

    /**
     * A class that handles the work for the server. It takes in a connection
     * from the server and does some work based off of input to the socket.
     */
    class Worker(socket: Socket) extends Runnable {

      // generic socket set up. ( used from the last lab)
      val outputStream = socket.getOutputStream
      val out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8")))
      val inStream = new InputStreamReader(socket.getInputStream)
      lazy val inVal = new BufferedReader(inStream)
      var recv = ""
      // variable to store messages.
      var count = 0
      val myConnection = connection


      /**
       * This is where the work of the worker is done, it checks the
       * message for either KILL_SERVICE or "HELO " as does tasks depending
       * on the input
       * - Replying with the desired string if HELO
       * - Or it kills the server if KILL_SERVICE
       */
      def run() {
        try {
          // if there is another message, get it.
          while (!socket.isClosed) {
            if (socket.getInputStream.available() > 0) {
              println("Waiting.... " + myConnection)
              recv = inVal.readLine()
              println("Received: " + recv)

              if (recv.contains("KILL_SERVICE")) handleKILL()
              else if(recv.contains("HELO ")) handleHELO()
              else
              recv match{
                case "SEARCH:" => handleSEARCH()
                case "GET_FILE_TIME:" => handGET_FILE_TIME()
                case "LS:" => handleSEARCH()
                case "DELETE_FILE:" => handleDELETE()
                case "GET_FILE:" => handleGET_FILE()
                case "WRITE_FILE:" => handleWRITE_FILE()
                case "RELEASE:" => handleRELEASE_FILE()
                case "" => println("Nothing")
                case _ => println("Error")
              }
             } //if
          } // end of while
        } catch {
          case s: SocketException => println("User pulled the plug")
        }
      }

      //Gets the file time from a file
      def handGET_FILE_TIME(): Unit = {
        var temp = inVal.readLine()
        val fileName = temp.split("--")(1) // The file name
        inVal.readLine() // this should be END;

        // if the file exists on the server, we can do some operations
        if (fileMap.fileExists(fileName)){
            // tell the node we want the timestamp of the file
            val nodeSocket = new Socket(HOST, fileMap.getPort(fileName))
            val nodeOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(nodeSocket.getOutputStream, "UTF-8")))
            val nodeInStream = new InputStreamReader(nodeSocket.getInputStream)
            lazy val nodeInVal = new BufferedReader(nodeInStream)
            nodeOut.println("GET_FILE_TIME:")
            nodeOut.println("TIME:--" + fileName)
            nodeOut.println("END;")
            nodeOut.flush()
            println("Sent the Time Stamp Request")

            // should be the header
            temp = nodeInVal.readLine()
            println(temp)
            if (temp != "ERROR - 99") {
              // gather up the contents
              val contents = nodeInVal.readLine().split("--")(1)
              temp = nodeInVal.readLine()
              //Now send the contents back to the user
              out.println("GET_FILE_TIME:")
              out.println("TIME:--" + contents)
              out.println("END;")
              out.flush()
              println("Sent time back to user " + contents)
            }
            else{
              out.println("ERROR - 99")
              out.println("END;")
              out.flush()
            }
        }
        else{
          out.println("ERROR - 99")
          out.println("END;")
          out.flush()
          println("No Such File;")
        }
      }

      // Deletes a file
      def handleDELETE(): Unit ={
        val temp = inVal.readLine()
        val fileName = temp.split("--")(1) // The file name
        inVal.readLine() // this should be END;

        // If the file exists, we can delete it.
        if(fileMap.fileExists(fileName)){
          val nodeSocket = new Socket(HOST, fileMap.getPort(fileName))
          val nodeOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(nodeSocket.getOutputStream, "UTF-8")))
          val nodeInStream = new InputStreamReader(nodeSocket.getInputStream)
          lazy val nodeInVal = new BufferedReader(nodeInStream)
          nodeOut.println("DELETE_FILE:")
          nodeOut.println("FILENAME:--" + fileName)
          nodeOut.println("END;")
          nodeOut.flush()
          println("Sent the DELETE Request To Nodes")

          // wait for an ACK
          if( nodeInVal.readLine() == "DELETE_FILE:"){
            val result = nodeInVal.readLine().split("--")(1)
            nodeInVal.readLine() // this is END;
            out.println("DELETE_FILE:")
            out.println("RESULT:--" + result)
            out.println("END;")
            out.flush()
            println("Sent the DELETE Request To client: "+result)
          }
        }
        else{
          out.println("ERROR - 99")
          out.println("END;")
          out.flush()
          println("No Such File;")
        }

      }

      // Handles a search for a file
      def handleSEARCH(): Unit = {
        var temp = inVal.readLine()
        val fileName = temp.split("--")(1) // The file name
        temp = inVal.readLine() // this should be END;
        if(fileMap.fileExists(fileName)){
          val nodeSocket = new Socket(HOST, fileMap.getPort(fileName))
          val nodeOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(nodeSocket.getOutputStream, "UTF-8")))
          val nodeInStream = new InputStreamReader(nodeSocket.getInputStream)
          lazy val nodeInVal = new BufferedReader(nodeInStream)
          nodeOut.println("SEARCH:")
          nodeOut.println("FILENAME:--" + fileName)
          nodeOut.println("END;")
          nodeOut.flush()
          println("Sent the SEARCH Request To Node")

          // wait for an ACK
          if( nodeInVal.readLine() == "SEARCH:"){
            nodeInVal.readLine().split("--")(1)
            nodeInVal.readLine()
            out.println("SEARCH:")
            out.println("IP:--" + HOST)
            out.println("PORT:--" + fileMap.getPort(fileName))
            out.println("END;")
            out.flush()
            println("Sent the SEARCH Request To client")
          }
          else{
            out.println("ERROR - 99")
            out.println("END;")
            out.flush()
            println("No Such File;")
          }
        }
        else{
          fileMap.addToMap(fileName)
          out.println("SEARCH:")
          out.println("IP:--" + HOST)
          out.println("PORT:--" + fileMap.getPort(fileName))
          out.println("END;")
          out.flush()
          println("New File; " + fileMap.getPort(fileName))
        }
        println("###################################################################")
      }

      // Tells the Node that the file is being released
      def handleRELEASE_FILE(): Unit = {
        var temp = inVal.readLine()
        val fileName = temp.split("--")(1) // The file name
        temp = inVal.readLine() // this should be END;
        if(fileMap.fileExists(fileName)){

          val nodeSocket = new Socket(HOST, fileMap.getPort(fileName))
          val nodeOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(nodeSocket.getOutputStream, "UTF-8")))
          val nodeInStream = new InputStreamReader(nodeSocket.getInputStream)
          lazy val nodeInVal = new BufferedReader(nodeInStream)
          nodeOut.println("RELEASE:")
          nodeOut.println("FILE_NAME:--" + fileName)
          nodeOut.println("END;")
          nodeOut.flush()
          println("Sent the Release Request")

          // wait for an ACK
          temp = nodeInVal.readLine()
          if (temp == "SUCCESS;") println("SUCCESS-RELEASE")
          else println("FAILURE-RELEASE")
        }
        else println("FAILURE-RELEASE")

        println("###################################################################")
      }

      // Writes to a specific Node
      def handleWRITE_FILE(): Unit = {
        println("Got a File Write Request")
        var temp = inVal.readLine()
        println(temp)

        val temp1 = temp.split("--")
        val fileName = temp1(1)
        var contents = List[String]()
        var response = ""
        //get the contents
        while(response != "END;"){
          response = inVal.readLine()
          if(response != "END;" && response != "CONTENTS:--"){
            temp = response.split("--")(1)
            println(temp)
            contents = contents ++ List(temp)
          }
          else if( response == "CONTENTS:--") contents = contents ++ List("\r\n")

        }
        fileMap.addToMap(fileName)

        if(!cache.isFileInCache(fileName)){

          // tell the node we want to write the file
          val nodeSocket = new Socket(HOST, fileMap.getPort(fileName))
          val nodeOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(nodeSocket.getOutputStream, "UTF-8")))
          val nodeInStream = new InputStreamReader(nodeSocket.getInputStream)
          lazy val nodeInVal = new BufferedReader(nodeInStream)


          // Tell the node we want to write to a file
          nodeOut.println("WRITE_FILE:")
          nodeOut.println("FILE_NAME:--" + fileName)

          for(s <- contents) nodeOut.println("CONTENTS:--" + s)

          nodeOut.println("END;")
          nodeOut.flush()
          println("Sent the File Request")

          // ACK back
          temp = nodeInVal.readLine()

          if (temp == "SUCCESS;"){
            cache.addToCache(new File(fileName) )
            cache.writeToFile(fileName,contents)
            out.println("SAVED: "+fileName)
            out.println("END;")
            out.flush()
            println("File Successfully written")
          }
          else println("FAILURE-WRITE")
        }
        else{
          println("Telling nodes to update from my Cache...")
          println(contents)
          cache.writeToFile(fileName, contents)
          // tell the node we want to write the file
          val nodeSocket = new Socket(HOST, fileMap.getPort(fileName))
          val nodeOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(nodeSocket.getOutputStream, "UTF-8")))
          val nodeInStream = new InputStreamReader(nodeSocket.getInputStream)
          lazy val nodeInVal = new BufferedReader(nodeInStream)

          // Tell the node we want to write to a file
          nodeOut.println("WRITE_FILE:")
          nodeOut.println("FILE_NAME:--" + fileName)

          for(s <- contents) nodeOut.println("CONTENTS:--" + s)

          nodeOut.println("END;")
          nodeOut.flush()
          println("Sent the File Request")

          // ACK back
          temp = nodeInVal.readLine()

          if (temp == "SUCCESS;"){
            cache.addToCache(new File(fileName) )
            cache.writeToFile(fileName,contents)
            out.println("SAVED: "+fileName)
            out.println("END;")
            out.flush()
            println("File Successfully written")
          }
          else println("FAILURE-WRITE")
        }
        println("###################################################################")

      }

      // Gets the contents of a file
      def handleGET_FILE(): Unit = {
        println("Got a File Read Request")
        val split = recv.split(" ")
        var temp = inVal.readLine()
        println(temp)
        val fileName = temp.split("--")(1)

        // this should be END;
        temp = inVal.readLine()
        println(temp)
        if(fileMap.fileExists(fileName)){
          if(!cache.isFileInCache(fileName)){
            // tell the node we want to write the file
            val nodeSocket = new Socket(HOST, fileMap.getPort(fileName))
            val nodeOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(nodeSocket.getOutputStream, "UTF-8")))
            val nodeInStream = new InputStreamReader(nodeSocket.getInputStream)
            lazy val nodeInVal = new BufferedReader(nodeInStream)
            nodeOut.println("GET_FILE:")
            nodeOut.println("FILE_NAME:--" + fileName)
            nodeOut.println("END;")
            nodeOut.flush()
            println("Sent the File Request")

            // should be the header
            temp = nodeInVal.readLine()
            println(temp)
            if (temp != "ERROR - 99") {
              // gather up the contents
              var contents = List[String]()
              var response = ""

              while(response != "END;"){
                response = inVal.readLine()
                if(response != "END;" && response != "CONTENTS:--"){
                  temp = response.split("--")(1)
                  println(temp)
                  contents = contents ++ List(temp)
                }
                else if( response == "CONTENTS:--") contents = contents ++ List("")
                else println("ERROR")
              }
              //Now send the contents back to the user
              out.println("FILE_CONTENTS:")
              for(l <- contents) out.println("CONTENTS:--" + l)
              out.println("END;")
              out.flush()
              println("Sent contents back to user " + contents)
            }
            else {
              out.println("ERROR - Cant get file, its in use")
              out.flush()
              println("ERROR - Cant get file, its in use")
            }
          }
          // The file is in the cache, so we can write back to the client.
          else{
            val theFile = cache.getFile(fileName)
            val fileInput = new FileInputStream(theFile)
            val contents = new Array[Byte](theFile.length().toInt)
            fileInput.read(contents)
            //Now send the contents back to the user
            val lines = Source.fromFile(fileName).getLines().toList

            out.println("FILE_CONTENTS:")
            for(l <- lines) out.println("CONTENTS:--" + l)

            out.println("END;")
            out.flush()
            println("Sent a copy of " + fileName + " from the cache.")
          }
        }
        else{
          out.println("ERROR - 99")
          out.println("END;")
          out.flush()
        }
        println("###################################################################")
      }

      // handles the killing of the server
      def handleKILL(): Unit = {
        println("KILLING_SERVICE through connection: " + myConnection)
        println(Thread.currentThread.getName + " is shutting down\n")
        shutdownServer() // call the shut down method
        println("###################################################################")
      }

      // method that handles HELO message
      def handleHELO(): Unit = {
        val messageWithoutHELO = recv + "\n"
        val ip = socket.getLocalAddress.toString.drop(1) + "\n"
        val port = serverSocket.getLocalPort + "\n"
        handleMessage(messageWithoutHELO + "IP:" + ip + "Port:" + port + "StudentID:ac7ce4082772456e04ad6d80cceff8ddc274a78fd3dc1f28fd05aafdc4665e1b")
        println("###################################################################")
      }

      // prints a message to all sockets
      def handleMessage(message: String): Unit = {
        for (s <- sockets) {
          val outputStream = s.getOutputStream
          val out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8")))
          out.println(message)
          out.flush()
        }
        println("###################################################################")
      }

      // This function kills the server
      def shutdownServer(): Unit = {
        try {
          if (serverSocket != null) {
            noKill = false
            handleMessage("KILL_SERVICE")
            threadPool.shutdownNow
            serverSocket.close()
          }
        } catch {
          case e: SocketException => println("Server shut down")
        }
      }
    }
  }

    // starts the server, must be satered with command line parameters for the port
    def main(args: Array[String]) {
      try {
        new Server(args(0).toInt).run()
      } catch {
        case outOfBounds: java.lang.ArrayIndexOutOfBoundsException =>
          println("Please provide command line arguments")
      }
    }

}
