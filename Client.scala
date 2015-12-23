import java.io._
import java.net.{Socket}
import scala.io._

object Client {
  // various member data to easily change
  // val host = "178.62.101.81"
  val host = "localhost"
  //val host = "localhost"
  val port = 8000

  def run(): Unit ={
    try{
        val socket = new Socket(host, port)
        val outputStream = socket.getOutputStream()
        lazy val in = new BufferedSource(socket.getInputStream()).getLines()
        val out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8")))

        // begin the first test
        println("Wrote to Files")
        out.println("WRITE_FILE:")
        out.println("FILE_NAME:--test.txt")
        out.println("CONTENTS:--newThings To say")
        out.println("END;")
        out.flush() // send the request to the server
        readLine() // wait for input to begin next test
       // out.println("WRITE_FILE test.txt newThings-To-say")
       // out.flush() // send the request to the server
        //out.println("FILE_WRITE test2.txt hi")
        //.flush() // send the request to the server
//        println("Releasing Access")
//        out.println("RELEASE:")
//        out.println("FILENAME:--test.txt")
//        out.println("END;")
//        out.flush() // send the request to the server
//
//        readLine() // wait for input to begin next test
        println("Getting Files")
        out.println("GET_FILE:")
        out.println("FILE_NAME:--test.txt")
        out.println("END;")
        out.flush() // send the request to the

//        println("Read Files")
//        out.println("FILE_READ test.txt")
//        out.flush() // send the request to the server
//        out.println("FILE_READ test2.txt")
//        out.flush() // send the request to the server
//
//
//        readLine() // wait for input to begin next test
//        println("LS")
//        out.println("LS")
//        out.flush() // send the request to the server


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
     Client.run()
  }
}