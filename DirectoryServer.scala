/**
 * Created by Simon on 05/12/2015.
 * Based off my previous labs
 */
import java.io._
import java.net._
import java.util.concurrent.Executors

object DirectoryServer {

  var connection = 0
  var ports = 8080
  var id = 0
  val numberOfNodes = 3
  val host = "localhost"

  var portsToFile : Map[String,Int] = Map()

  val cache = new Cache
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
              // If its a HELO message
              else if (recv.contains("HELO ")) handleHELO()
              else if (recv == "SEARCH:" ) handleSEARCH()
              else if (recv == "GET_FILE:") handleGET_FILE()
              else if (recv == "WRITE_FILE:") handleWRITE_FILE()
              else if (recv == "RELEASE:") handleRELEASE_FILE()
              else if (recv == "") println("Nothing")
              else println("Hello")

            } //if
          } // end of while
        } catch {
          case s: SocketException => println("User pulled the plug")
        }
      }

      def handleSEARCH(): Unit = {
        var temp = inVal.readLine()
        val fileName = temp.split("--")(1) // The file name
        temp = inVal.readLine() // this should be END;

        val nodeSocket = new Socket("localhost", 8080)
        val nodeOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(nodeSocket.getOutputStream, "UTF-8")))
        val nodeInStream = new InputStreamReader(nodeSocket.getInputStream)
        lazy val nodeInVal = new BufferedReader(nodeInStream)
        nodeOut.println("SEARCH:")
        nodeOut.println("FILENAME:--" + fileName)
        nodeOut.println("END;")
        nodeOut.flush()
        println("Sent the SEARCH Request To Nodes")

        // wait for an ACK
        if( nodeInVal.readLine() == "SEARCH:"){
          val fileLocation = nodeInVal.readLine().split("--")(1)
          nodeInVal.readLine()
          out.println("SEARCH:")
          out.println("FILEPATH:--" + fileLocation)
          out.println("END;")
          out.flush()
          println("Sent the SEARCH Request To client")
        }
        else{
          out.println("No Such File;")
          out.flush()
          println("No Such File;")
        }
        println("###################################################################")
      }

      // Tells the Node that the file is being released
      def handleRELEASE_FILE(): Unit = {
        var temp = inVal.readLine()
        val fileName = temp.split("--")(1) // The file name
        temp = inVal.readLine() // this should be END;

        val nodeSocket = new Socket("localhost", 8080)
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

        if (temp == "SUCCESS;") {
          println("SUCCESS-RELEASE")
        }
        else {
          println("FAILURE-RELEASE")
        }
        println("###################################################################")
      }

      // Writes to a specific Node
      def handleWRITE_FILE(): Unit = {
        println("Got a File Write Request")
        var temp = inVal.readLine()
        println(temp)

        var temp1 = temp.split("--")
        val fileName = temp1(1)
        //get the contents
        temp = inVal.readLine()
        temp1 = temp.split("--")
        val contents = temp1(1)
        println(temp)

        // this should be END;
        temp = inVal.readLine()
        println(temp)
        if(!cache.isFileInCache(fileName)){

          // tell the node we want to write the file
          val nodeSocket = new Socket("localhost", 8080)
          val nodeOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(nodeSocket.getOutputStream, "UTF-8")))
          val nodeInStream = new InputStreamReader(nodeSocket.getInputStream)
          lazy val nodeInVal = new BufferedReader(nodeInStream)
          nodeOut.println("WRITE_FILE:")
          nodeOut.println("FILE_NAME:--" + fileName)
          nodeOut.println("CONTENTS:--" + contents)
          nodeOut.println("END;")
          nodeOut.flush()
          println("Sent the File Request")

          // ACK back
          temp = nodeInVal.readLine()

          if (temp == "SUCCESS;"){
            cache.addToCache(new File(fileName) )
            out.println("SAVED: "+fileName)
            out.println("END;")
            out.flush()
            println("File Successfully written")
          }
          else println("FAILURE-WRITE")

        }
        else{
          println("Telling nodes to update from my Cache...")
          cache.writeToFile(fileName, contents)
          val nodeSocket = new Socket("localhost", 8080)
          val nodeOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(nodeSocket.getOutputStream, "UTF-8")))
          val nodeInStream = new InputStreamReader(nodeSocket.getInputStream)
          lazy val nodeInVal = new BufferedReader(nodeInStream)
          nodeOut.println("WRITE_FILE:")
          nodeOut.println("FILE_NAME:--" + fileName)
          nodeOut.println("CONTENTS:--" + contents)
          nodeOut.println("END;")
          nodeOut.flush()
          println("Sent the File Request from Cache...")
          // ACK back
          temp = nodeInVal.readLine()

          if (temp == "SUCCESS;"){
            out.println("SAVED: "+fileName)
            out.println("END;")
            out.flush()
            println("Nodes Updated their copy")
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
        if(!cache.isFileInCache(fileName)){
          // tell the node we want to write the file
          val nodeSocket = new Socket("localhost", 8080)
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
            val contents = nodeInVal.readLine()
            println(contents)
            temp = nodeInVal.readLine()
            println(temp)

            //Now send the contents back to the user
            out.println("FILE_CONTENTS:")
            out.println("CONTENTS:" + contents)
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
        else{
          val theFile = cache.getFile(fileName)
          val fileInput = new FileInputStream(theFile)
          val contents = new Array[Byte](theFile.length().toInt)
          fileInput.read(contents)
          val toSendBack = new String(contents)
          //Now send the contents back to the user
          out.println("FILE_CONTENTS:")
          out.println("CONTENTS:" + contents)
          out.println("END;")
          out.flush()
          println("Sent a copy of " + fileName + " from the cache.")
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
