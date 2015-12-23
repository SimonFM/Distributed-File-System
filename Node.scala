import java.io._
import java.net.{ServerSocket, Socket, SocketException}
import java.util.concurrent.Executors

object Node {

  /**
   * A class that handles the work for the server. It takes in a connection
   * from the server and does some work based off of input to the socket.
   */
  class NodeServer(portNumber: Int) extends Runnable {
    var NUMBER_OF_THREADS = 20
    // Maximum number of threads.
    val serverSocket = new ServerSocket(portNumber) // setting up the server
    var port = portNumber
    println("Node running on port number: " + portNumber)
    // display to console
    val threadPool = Executors.newFixedThreadPool(NUMBER_OF_THREADS) // create the thread pool

    var sockets: List[Socket] = List()
    var noKill = true
    val fileManager = new FileManager()
    // Empty hash table whose keys are strings and values are integers:
    var A:Map[String,Int] = Map()

    /**
     * This is the run method of the server, it is needed as I have extended my server
     * to be Runnable, so I could have multiple servers should the need arise.
     * It creates a new socket for every new connection to the server.
     * It loops forever, as long as the server is not closed.
     */
    def run(): Unit = {
      try {
        println("Nodes is running on port: " + portNumber)
        while (!serverSocket.isClosed && noKill) {
          try {
            sockets = serverSocket.accept() :: sockets
            if (sockets.nonEmpty) {
              println("New Client requested to connect")
              threadPool.execute(new NodeWorker(sockets.head)) // allocate a new Worker a Socket
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

    class NodeWorker(socket: Socket) extends Runnable {
      // generic socket set up. ( used from the last lab)
      val outputStream = socket.getOutputStream
      val outVal = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8")))
      val inStream = new InputStreamReader(socket.getInputStream)
      lazy val inVal = new BufferedReader(inStream)
      var recv = ""
      // variable to store messages.
      var count = 0


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
              println("Waiting.... ")
              recv = inVal.readLine()
              println("Received: " + recv)
              if (recv .contains("FILE_READ ")) handleFILE_READ()
             // else if (recv .contains("FILE_GET ")) handleFILE_GET()
             // else if (recv .contains("FILE_WRITE ")) handleFILE_WRITE()
              else if (recv == "WRITE_FILE:") handleWRITE_FILE()
              else if (recv == "GET_FILE:") handleGET_FILE()
              else if (recv .contains("LS")) handleLS()
              else if (recv == "") print("Nothing")
              else {println("Hello")}
            } //if
          } // end of while
        } catch {
          case s: SocketException => println("User pulled the plug")
        }
      }

      def handleFILE_READ(): Unit = {
        val split = recv.split(" ")
        val contents = fileManager.getFileContents(split(1))
        val out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream, "UTF-8")))
        println("File Contents: " + contents)
        out.println(contents)
        out.println("READ")
        out.flush()
        println("Sent Back File Contents")
      }

      def handleFILE_GET(): Unit = {
        val split = recv.split(" ")
        fileManager.writeToFile(split(1), split(2))
      }
      def handleWRITE_FILE(): Unit = {

        val split = recv.split(" ")
        var temp = inVal.readLine()
        println("Hello0")

        var temp1 = temp.split("--")
        val fileName = temp1(1)

        //get the contents
        temp = inVal.readLine()
        println("Hello1")

        temp1 = temp.split("--")
        val contents = temp1(1)

        // this should be END;
        temp = inVal.readLine()
        println("Hello3")

        //val split = recv.split(" ")
        //val fName = split(1)
        //val toBeWritten = split(2)
        val output = socket.getOutputStream
        if(!fileManager.isFileBeingWrittenTo(fileName)){
          val bytesToBeWritten = contents.getBytes
          val file = fileManager.getFile(fileName)
          if(file != null){
            val fStream = new FileOutputStream(fileName)
            fStream.write(bytesToBeWritten)
            fStream.close()

            outVal.println("SUCCESS;")
            outVal.flush()
            println("Sent SUCCESS;")
            fileManager.releaseFile(fileName)
          }
          else{
            outVal.println("FAILURE;")
            outVal.flush()
            println("FAILURE;" )
          }

        }
        else{
          outVal.println("Someone else in using that file")
          outVal.flush()
        }
      }

      // Gets the contents of a file
      def handleGET_FILE(): Unit = {



        val split = recv.split(" ")
        var temp = inVal.readLine()
        var temp1 = temp.split("--")
        val fileName = temp1(1)
        println(temp)

        // this should be END;
        temp = inVal.readLine()
        println(temp)

        if(!fileManager.isFileBeingWrittenTo(fileName)){
          val theFile = fileManager.getFile(fileName)
          val fileInput = new FileInputStream(theFile)
          println("Got a File Request " + fileName)

          val contents = new Array[Byte](theFile.length().toInt)

          fileInput.read(contents)
          val toSendBack = new String(contents)
          outVal.println("FILE_CONTENTS:")
          outVal.println("CONTENTS:" + toSendBack)
          outVal.println("END;")
          outVal.flush()
          println("Sent back File")

        }
        else{
          outVal.println("ERROR - 99")
          outVal.flush()
          println("Sent The Error")
        }
      }

      // performs LS in the Node
      def handleLS(): Unit ={
        val contents = fileManager.lsCommand()
        var results = ""
        val out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream, "UTF-8")))

        for(file <- contents) results = results + file + "\n"

        println(results)
        if(results == "") {
          out.println("Nope nothing :(")
          out.println("LS")
          out.flush()
          println("Sent Back Nope LS")
        } else {
          out.println(results)
          out.println("LS")
          out.flush()
          println("Sent Back LS")
        }
      }

      // Handles writes (in th form of strings) to a file
      def handleFILE_WRITE(): Unit = {
        val split = recv.split(" ")
        if(fileManager.writeToFile(split(1),split(2))){
          val out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream, "UTF-8")))
          out.println("Written To File: "+ split(1))
          out.flush()
          println("Written To File: "+ split(1))
        }
        else{
          outVal.println("Someone else has control of the file: "+ split(1))
          outVal.flush()
          println("Someone else has control of the file: "+ split(1))
        }

      }

      // prints a message to all sockets
      def handleMessage(message: String): Unit = {
        for (s <- sockets) {
          val outputStream = s.getOutputStream
          val out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8")))
          out.println(message)
          out.flush()
        }
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

  var nodes: List[NodeServer] = List()
  val host = "localhost"
  var port = 8080
  def createNodes(size: Int): Unit = {
    for (i <- 5 to size) {
      nodes = new NodeServer(port) :: nodes
      port = port + 1

    }
    println("Nodes Created...")
  }

  def startNodes(size: Int): Unit = {
    for (n <- nodes) n.run()
    println("Nodes are running...")
  }
  // Main method that runs the program
  def main(args: Array[String]) {
    //val input = readLine("Please Enter in the port to start on: ")
    new NodeServer(8080).run()
  }

}






