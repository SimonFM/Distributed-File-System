import java.io._
import java.net.Socket

object Test {
  // various member data to easily change
  // val host = "178.62.101.81"
  val host = "localhost"
  //val host = "localhost"
  val port = 8000

  def run(): Unit ={
    try{
        val socket = new Socket(host, port)
        val outputStream = socket.getOutputStream()
        val inStream = new InputStreamReader(socket.getInputStream)
        lazy val inVal = new BufferedReader(inStream)
        val out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8")))

        // begin the first test
        println("Wrote to Files")
        out.println("WRITE_FILE:")
        out.println("FILE_NAME:--test.txt")
        out.println("CONTENTS:--newThings To say")
        out.println("END;")
        out.flush() // send the request to the server
        println("Received")

        println(inVal.readLine())
        println(inVal.readLine())

        out.println("WRITE_FILE:")
        out.println("FILE_NAME:--test2.txt")
        out.println("CONTENTS:--Whats up Stephen")
        out.println("END;")
        out.flush() // send the request to the server
        println("Received")

        println(inVal.readLine())
        println(inVal.readLine())
        println("#######################################################")
        readLine() // wait for input to begin next test

        println("Getting Files")
        out.println("GET_FILE:")
        out.println("FILE_NAME:--test.txt")
        out.println("END;")
        out.flush() // send the request to the

        println("Received:")
        println(inVal.readLine())
        println(inVal.readLine())
        println(inVal.readLine())
        println("#######################################################")

        readLine() // wait for input to begin next test
        println("Releasing Files")
        out.println("RELEASE:")
        out.println("FILE_NAME:--test.txt")
        out.println("END;")
        out.flush() // send the request to the
        println("#######################################################")

        readLine() // wait for input to begin next test
        println("Wrote to Files Again")
        out.println("WRITE_FILE:")
        out.println("FILE_NAME:--test.txt")
        out.println("CONTENTS:--I wrote from the cache")
        out.println("END;")
        out.flush() // send the request to the server

        println("Received:")
        println(inVal.readLine())
        println(inVal.readLine())
        println("#######################################################")

        readLine() // wait for input to begin next test
        println("SEARCH:")
        out.println("SEARCH:")
        out.println("FILEPATH:--test.txt")
        out.println("END;")
        out.flush() // send the request to the server

        println("Received:")
        println(inVal.readLine())
        println(inVal.readLine())
        println(inVal.readLine())
        println("#######################################################")
        readLine()
        println("DELETE1:")
        out.println("DELETE_FILE:")
        out.println("FILE_NAME:--test.txt")
        out.println("END;")
        out.flush() // send the request to the server

        println("Received1:")
        println(inVal.readLine())
        println(inVal.readLine())
        println(inVal.readLine())

        println("DELETE2:")
        out.println("DELETE_FILE:")
        out.println("FILE_NAME:--test2.txt")
        out.println("END;")
        out.flush() // send the request to the server

        println("Received2:")
        println(inVal.readLine())
        println(inVal.readLine())
        println(inVal.readLine())
        println("#######################################################")
        readLine()

        socket.close()
        println("Socket Closed!")

    }catch{
      case notConnected : java.net.ConnectException =>
        println("Can't connect to: "+host+" on port: "+port)
    }

  }
  // Main method that runs the program
  def main(args: Array[String]) {
     Test.run()
  }
}