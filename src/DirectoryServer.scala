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

  val cache = new FileManager()
  cache.maxSize = 10
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
            if (!sockets.isEmpty) {
              println("New Client requested to connect")
              threadPool.execute(new Worker(sockets.head)) // allocate a new Worker a Socket
              connection = connection + 1
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



    /**
     * A class that handles the work for the server. It takes in a connection
     * from the server and does some work based off of input to the socket.
     */
    class Worker(socket: Socket) extends Runnable {

      // generic socket set up. ( used from the last lab)
      val outputStream = socket.getOutputStream()
      val out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8")))
      val inStream = new InputStreamReader(socket.getInputStream)
      lazy val in = new BufferedReader(inStream)
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
            if (socket.getInputStream().available() > 0) {
              println("Waiting.... " + myConnection)
              recv = in.readLine()
              println("Received: " + recv)
              if (recv.contains("KILL_SERVICE")) handleKILL
              // If its a HELO message
              else if (recv.contains("HELO ")) handleHELO
              // without this line this causes me to get 68
              else if (recv .contains("LS")) handleLS
              else if (recv .contains("FILE_READ ")) handleFILE_READ
              else if (recv .contains("FILE_GET ")) handleFILE_GET
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

      def checkCacheREAD(filename : String): Boolean={
        if(!cache.containsFile(filename)) return false

        val contents = cache.getFileContents(filename)
        val out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream, "UTF-8")))

        out.println(contents)
        out.println("READ")
        out.flush()

        return true
      }

      def checkCacheWRITE(filename : String, contents : String): Unit={
        if(!cache.containsFile(filename)) println("Not in cache")
        else{
          cache.writeToFile(filename,contents)
          val out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream, "UTF-8")))

          out.println(contents)
          out.println("READ")
          out.flush()
        }

      }

      def handleFILE_READ(): Unit ={
        // connect to the node with the file
        val split = recv.split(" ")
        val fileNameKey = split(1)

        var temp = 0
        if(checkCacheREAD(fileNameKey)){
          println("Cache Hit" + fileNameKey)
          println("Fetched from Cache")
        }
        else{
          if(!portsToFile.contains(fileNameKey)) {
            val out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8")))
            out.println("No Such File")
            out.flush()
          }
          else{
            var contents = ""
            val nodeSocket = new Socket("localhost",portsToFile(fileNameKey))
            val nodeInput = nodeSocket.getInputStream
            val nodeOutput = nodeSocket.getOutputStream
            val nodeOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(nodeOutput, "UTF-8")))
            val nodeInStream = new InputStreamReader(nodeInput)
            lazy val nodeIn = new BufferedReader(nodeInStream)

            nodeOut.println(recv)
            nodeOut.flush()

            val out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8")))

            while(!contents.contains("READ")) {
              contents += nodeIn.readLine() + "\n"
            }
            println("Recieved: " + contents)
            out.println(contents)
            out.flush()
          }
        }
      }

      def handleLS(): Unit ={
        // connect to the node with the file
        val temp = 0
        val split = recv.split(" ")

        val out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8")))

        portsToFile.keys.foreach{
          key => out.println(key)
               out.flush()
               println(key)
        }
        println("Sent the user back the Files from LS")
      }

      def handleFILE_GET(): Unit ={

      }

      def handleFILE_WRITE(): Unit ={
        val split = recv.split(" ")
        val fileNameKey = split(1)
        var temp = 0
        if(!portsToFile.contains(fileNameKey)){
          if(ports >= 8082){
            ports = 8080
            temp = ports
          }
          else{
            temp = ports
            ports = ports + 1
          }
          val tempMap : Map[String,Int]  =  Map(fileNameKey -> temp)
          portsToFile = portsToFile ++ tempMap
          println("Its a new file")

        }
        val nodeSocket = new Socket("localhost",portsToFile(fileNameKey))
        val nodeOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(nodeSocket.getOutputStream(), "UTF-8")))
        nodeOut.println(recv)
        nodeOut.flush()
        println("Told Node to write")
        cache.writeToFile(split(1),split(2))
      }

      // handles the killing of the server
      def handleKILL(): Unit = {
        println("KILLING_SERVICE through connection: " + myConnection)
        println(Thread.currentThread.getName() + " is shutting down\n")
        shutdownServer() // call the shut down method
      }

      // method that handles HELO message
      def handleHELO(): Unit = {
        val messageWithoutHELO = recv + "\n"
        val ip = socket.getLocalAddress().toString().drop(1) + "\n"
        val port = serverSocket.getLocalPort + "\n"
        handleMessage(messageWithoutHELO + "IP:" + ip + "Port:" + port + "StudentID:ac7ce4082772456e04ad6d80cceff8ddc274a78fd3dc1f28fd05aafdc4665e1b")
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
