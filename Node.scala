import java.io._
import java.net.{ServerSocket, Socket, SocketException}
import java.util.concurrent.Executors

import scala.io.Source

object Node {
  /**
   * A class that handles the work for the server. It takes in a connection
   * from the server and does some work based off of input to the socket.
   */
  class NodeServer(portNumber: Int) extends Runnable {
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
    var A:Map[String,Int] = Map()

    var NODE_NAME = ""
    var folder = ""

    def makeDirectory(folderName : String): Unit ={
      val theDir = new File(folderName)
      if(!theDir.exists()){
        try {
          theDir.mkdirs()
        }catch{
          case secE: SecurityException  => println("Sorry you're not allowed to make folders")
        }
        println("Made a new Directory")
      }
    }

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
        makeDirectory("Node-"+PORT)
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

              recv match{
                case "WRITE_FILE:" => handleWRITE_FILE()
                case "GET_FILE:" => handleGET_FILE()
                case "GET_FILE_TIME:" => handleGET_FILE_TIME()
                case "DELETE_FILE:" => handleDELETE()
                case "RELEASE:" => handleRELEASE_FILE()
                case "SEARCH:" => handleSEARCH()
                case "" => println("Nothing was received")
                case _ => println("Invalid State")

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
          val f = new File("Node-"+PORT+"/"+fileName)
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
        if(fileManager.containsFile(fileName) && !fileManager.isFileBeingWrittenTo(fileName) ){
          val file = new File("Node-"+PORT+"/"+fileName)

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

      // Tells the replicators that they are being written to
      // and the data that was just written to this node is also written to
      // the replicator nodes too.
      def writeToNodes(fileName : String, contents : List[String] ): Unit ={
        var port = 9000
        while(port < 9003){
          // tell the node we want to write the file
          val nodeSocket = new Socket(HOST, port)
          val nodeOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(nodeSocket.getOutputStream, "UTF-8")))
          val nodeInStream = new InputStreamReader(nodeSocket.getInputStream)
          lazy val nodeInVal = new BufferedReader(nodeInStream)
          nodeOut.println("WRITE_FILE:")
          nodeOut.println("FILE_NAME:--" + fileName)
          for(s <- contents)
            nodeOut.println("CONTENTS:--" + s)

          nodeOut.println("END;")
          nodeOut.flush()
          println("Sent the File Request")

          // ACK back
          if ( nodeInVal.readLine() == "SUCCESS;"){
            outVal.println("SAVED: "+fileName)
            outVal.println("END;")
            outVal.flush()
            println("File Successfully written")
          }
          else println("FAILURE-WRITE")
          port = port + 1
        }
      }

      // Tells the node it is getting a write request
      def handleWRITE_FILE(): Unit = {

        val fileName = inVal.readLine().split("--")(1)
        println(fileName)

        var temp = ""
        var contents = List[String]()
        var response = ""
        //get the contents
        while(response != "END;"){
          response = inVal.readLine()
          // if the current line is not END & is not an empty line
          // then add it to the current file
          if(response != "END;" && response != "CONTENTS:--"){
            temp = response.split("--")(1)
            println(temp)
            contents = contents ++ List(temp)
          }
            // else if it is a empty line, add in an empty line to the file
          else if( response == "CONTENTS:--")  contents = contents ++ List("\n")
        }
        val file = new File("Node-"+PORT+"/"+fileName)
        if(file.exists()){
          val output = socket.getOutputStream
          // if the file isn't locked already
          if(!fileManager.isFileBeingWrittenTo(fileName)) {
            // obtain the lock and then release
            fileManager.lock(fileName)
            fileManager.addFile(fileName)
            val writer = new PrintWriter(file)
            for (l <- contents) {
              println("Contents: "+l)
              if("" != l){
                writer.write(l+"\n")
                writer.flush()
              }
            }
            writer.close()
            outVal.println("SAVED: "+fileName)
            outVal.println("END;")
            outVal.flush()
            fileManager.releaseFile(fileName)
            println("Sent SAVED;")
            writeToNodes(fileName,contents) //perform replication
          }
          else{
            outVal.println("FAILURE;")
            outVal.flush()
            println("FAILURE;" )
          }
        }
        else{ // The file does'nt exist and we need to make it
          fileManager.lock(fileName)
          fileManager.addFile(fileName)
          val writer = new PrintWriter(file)
          for (l <- contents) {
            println("Contents: "+l)
            if("" != l){
              writer.write(l+"\n")
              writer.flush()
            }
          }
          writer.close()
          outVal.println("SAVED: "+fileName)
          outVal.println("END;")
          outVal.flush()
          fileManager.releaseFile(fileName)
          println("Sent SAVED;")
          writeToNodes(fileName,contents)//perform replication

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
        val theFile = new File ("Node-"+PORT+"/"+fileName)
        if(theFile.exists()){
          if(!fileManager.isFileBeingWrittenTo(fileName)){

            println("Got a File Request " + fileName)
            val lines = Source.fromFile("Node-"+PORT+"/"+fileName).getLines().toList

            outVal.println("FILE_CONTENTS:")
            for(l <- lines) outVal.println("CONTENTS:--" + l)
            outVal.println("END;")
            outVal.flush()
            println("Sent back File")
          }
          // It is under control of someone else.
          else{
            outVal.println("ERROR-99")
            outVal.flush()
            println("Sent The Error")
          }
        }
        // Not in the system
        else{
          outVal.println("ERROR-99")
          outVal.flush()
          println("File Doesn't Exist")
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

        if(fileManager.containsFile("Node-"+PORT+"/"+fileName)){
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
  val HOST = "localhost"
  var PORT = 8080

  // Main method that runs the program
  def main(args: Array[String]) {
    val input = readLine("Please Enter in the port to start on: ")
    val node = new NodeServer(input.toInt)
    node.NODE_NAME = "NODE:"+input
    node.run()
  }

}






