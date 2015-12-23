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
              if (recv == "WRITE_FILE:") handleWRITE_FILE()
              else if (recv == "GET_FILE:") handleGET_FILE()
              else if (recv == "DELETE_FILE:" ) handleDELETE()
              else if (recv  == "RELEASE:") handleRELEASE_FILE()
              else if (recv  == "SEARCH:") handleSEARCH()
              else if (recv == "") print("Nothing")
              else {println("Hello")}
            } //if
          } // end of while
        } catch {
          case s: SocketException => println("User pulled the plug")
        }
      }

      def handleDELETE(): Unit ={
        val fileName = inVal.readLine().split("--")(1)
        inVal.readLine()// this should be END;
        if(fileManager.containsFile(fileName) && !fileManager.isFileBeingWrittenTo(fileName) ){
          val file = fileManager.getFile(fileName)

          outVal.println("DELETE_FILE:")
          val result = file.delete()
          if(result) outVal.println("RESULT:--SUCCESS")
          else outVal.println("RESULT:--FAILURE")
          outVal.println("END;")
          outVal.flush()
          println("Sent the result of the delete back to the client "+result  )
        }
        else{
          outVal.println("DELETE_FILE:")
          outVal.println("RESULT:--FAILURE")
          outVal.println("END;")
          outVal.flush()
          println("No Such File; Server"+portNumber+":"+fileName)
        }
        println("###################################################################")
      }
      // Tells the node it is getting a write request
      def handleWRITE_FILE(): Unit = {

        val split = recv.split(" ")
        var temp = inVal.readLine()
        println(temp)

        var temp1 = temp.split("--")
        val fileName = temp1(1)

        //get the contents
        temp = inVal.readLine()
        println(temp)

        temp1 = temp.split("--")
        val contents = temp1(1)

        // this should be END;
        temp = inVal.readLine()
        println(temp)

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
        println("###################################################################")
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
        println("###################################################################")
      }

      // Tells the node that someone has released access
      def handleRELEASE_FILE(): Unit ={
        var temp = inVal.readLine()
        var temp1 = temp.split("--")
        val fileName = temp1(1) // The file name
        temp = inVal.readLine() // this should be END;

        fileManager.releaseFile(fileName)

        outVal.println("SUCCESS;")
        outVal.flush()
        println("File was released")
        println("###################################################################")

      }

      // performs LS in the Node
      def handleSEARCH(): Unit ={
        val fileName = inVal.readLine().split("--")(1)
        inVal.readLine()// this should be END;

        if(fileManager.containsFile(fileName)){
          outVal.println("SEARCH:")
          outVal.println("FILEPATH:--Server"+portNumber+":"+fileName)
          outVal.println("END;")
          outVal.flush()
          println("Sent File Location back: Server"+portNumber+":"+fileName )
        }
        else{
          outVal.println("NOT_HERE:")
          outVal.flush()
          println("No Such File; Server"+portNumber+":"+fileName)
        }
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
  // Main method that runs the program
  def main(args: Array[String]) {
    val input = readLine("Please Enter in the port to start on: ")
    new NodeServer(input.toInt).run()
  }

}






