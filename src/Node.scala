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
            if (!sockets.isEmpty) {
              println("New Client requested to connect")
              threadPool.execute(new NodeWorker(sockets.head)) // allocate a new Worker a Socket
            }
            else println("Empty socket list")
          } catch {
            case socketE: SocketException =>
              serverSocket.close
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
      val outputStream = socket.getOutputStream()
      val out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8")))
      val inStream = new InputStreamReader(socket.getInputStream)
      lazy val in = new BufferedReader(inStream)
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
            if (socket.getInputStream().available() > 0) {
              println("Waiting.... ")
              recv = in.readLine()
              println("Received: " + recv)
              if (recv .contains("FILE_READ ")) handleFILE_READ
              else if (recv .contains("FILE_GET ")) handleFILE_GET
              else if (recv .contains("LS")) handleLS
              else if (recv .contains("FILE_WRITE ")) handleFILE_WRITE
              else if (recv == "") print("Nothing")
              else {

              }
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

      def handleLS(): Unit ={
        val contents = fileManager.lsCommand()
        var results = ""
        val out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream, "UTF-8")))

        for(file <- contents){
          results = results + file + "\n"
        }
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

      def handleFILE_WRITE(): Unit = {
        val split = recv.split(" ")
        fileManager.writeToFile(split(1),split(2))
        val out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream, "UTF-8")))
        out.println("Written To File: "+ split(1))
        out.flush()
        println("Written To File: "+ split(1))
      }

      // prints a message to all sockets
      def handleMessage(message: String): Unit = {
        for (s <- sockets) {
          val outputStream = s.getOutputStream()
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
            serverSocket.close
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
    val input = readLine("Please Enter in the port to start on: ")
    new NodeServer(input.toInt).run()
  }

}






