import java.io._
import java.net.{ServerSocket, Socket, SocketException}
import java.util.concurrent.Executors

object ReplicatedNode {
  /**
   * A class that handles the work for the server. It takes in a connection
   * from the server and does some work based off of input to the socket.
   */
  class ReplicatedNodeServer(portNumber: Int) extends Runnable {
    var NUMBER_OF_THREADS = 20  // Maximum number of threads.
    var PORT = portNumber // save the port number
    val serverSocket = new ServerSocket(PORT) // setting up the server
    println("Node running on port number: " + PORT)
    // display to console
    val threadPool = Executors.newFixedThreadPool(NUMBER_OF_THREADS) // create the thread pool

    var sockets: List[Socket] = List()
    var noKill = true
    val fileManager = new FileManager()
    // Empty hash table whose keys are strings and values are integers:
    var A : Map[String,Int] = Map()

    var NODE_NAME = ""
    var folder = ""
    var LOCATION = ""


    // Creates a folder called whatever the sever is called
    def makeDirectory(folderName : String): Unit ={
      val theDir = new File(folderName)
      LOCATION = folderName
      if(!theDir.exists()){
        try {theDir.mkdirs()}
        catch{
          case secE: SecurityException  => println("Sorry you're not allowed to make folders")
        }
        println("Made a new Directory")
      }
    }

    // Creates folder called Cache
    def makeCacheFolder(): Unit ={
      makeDirectory(NODE_NAME + "/" + "cache")
      folder = NODE_NAME + "/" + "cache"
    }

    /**
     * This is the run method of the server, it is needed as I have extended my server
     * to be Runnable, so I could have multiple servers should the need arise.
     * It creates a new socket for every new connection to the server.
     * It loops forever, as long as the server is not closed.
     */
    def run(): Unit = {
      try {
        makeDirectory("Replicated"+PORT)
        println("Nodes is running on port: " + portNumber)
        while (!serverSocket.isClosed && noKill) {
          try {
            sockets = serverSocket.accept() :: sockets
            if (sockets.nonEmpty) {
              println("New Client requested to connect")
              threadPool.execute(new ReplicatedNodeWorker(sockets.head)) // allocate a new Worker a Socket
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

    class ReplicatedNodeWorker(socket: Socket) extends Runnable {
      // generic socket set up. ( used from the last lab)
      val outputStream = socket.getOutputStream
      val outVal = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8")))
      val inStream = new InputStreamReader(socket.getInputStream)
      lazy val inVal = new BufferedReader(inStream)
      var recv = ""// variable to store messages.
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

              recv match{
                case "WRITE_FILE:" => handleWRITE_FILE()
                case "DELETE_FILE:" =>  handleDELETE()
                case "" => println("Nothing")
                case _ => println("Error State")
              }
            } //if
          } // end of while
        } catch {
          case s: SocketException => println("User pulled the plug")
        }
      }

      //get the timeStamp from a file
      def handleGET_FILE_TIME(): Unit ={
        val fileName = inVal.readLine().split("--")(1) // The file name
        inVal.readLine()
        if(fileManager.containsFile(fileName)){
          val f = new File(fileName)
          outVal.println("GET_FILE_TIME:")
          outVal.println("TIME:--" + f.lastModified())
          outVal.println("END;")
          outVal.flush()
          println("Sent time back to user " + f.lastModified())
        }
        else{
          outVal.println("ERROR - 99")
          outVal.println("END;")
          outVal.flush()
        }
      }

      // Deletes a file
      def handleDELETE(): Unit ={
        val fileName = inVal.readLine().split("--")(1)
        inVal.readLine()// this should be END;
        // if the file isn't being written to and isn't locked, then delte
        if(fileManager.containsFile(fileName) && !fileManager.isFileBeingWrittenTo(fileName) ){
          val file = new File(LOCATION+"/"+fileName)
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
        val fileName = inVal.readLine().split("--")(1)
        var temp = ""
        var contents = List[String]()
        var response = ""
        //get the contents
        while(response != "END;"){
          response = inVal.readLine()
          println(response)
          if(response != "END;" && response != "CONTENTS:--"){
            val reponseSplit = response.split("--")
            if(reponseSplit.size > 1){
              temp = reponseSplit(1)
              println(temp)
              contents = contents ++ List(temp)
            }
          }
          else if( response == "CONTENTS:--") contents = contents ++ List("\n")
          else println("")
        }

        val output = socket.getOutputStream
        if(!fileManager.isFileBeingWrittenTo(fileName)) {
          val writer = new PrintWriter(new File(LOCATION+"/"+fileName))
          for (l <- contents) {
            println("Contents: "+l)
            if("" != l){
              writer.write(l + "\n")
              writer.flush()
            }
          }
          writer.close() //  close writing to the file
          // send back a success message
          outVal.println("SUCCESS;")
          outVal.flush()
          fileManager.releaseFile(fileName)
        }
        else{
          outVal.println("FAILURE;")
          outVal.flush()
          println("FAILURE;" )
        }
        println("###################################################################")
      }

      // This function kills the server, from lab 3... or was it 4
      def shutdownServer(): Unit = {
        try {
          if (serverSocket != null) {
            noKill = false
            threadPool.shutdownNow
            serverSocket.close()
          }
        } catch {
          case e: SocketException => println("Server shut down")
        }
      }
    }
  }

  var nodes: List[ReplicatedNodeServer] = List()
  val HOST = "localhost"
  var PORT = 8080

  // Main method that runs the program
  def main(args: Array[String]) {
    val input = readLine("Please Enter in the port to start on: ")
    val node = new ReplicatedNodeServer(input.toInt)
    node.NODE_NAME = "NODE:"+input
    node.run()
  }

}






