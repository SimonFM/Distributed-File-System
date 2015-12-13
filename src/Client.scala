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
      while(true) {


        val socket = new Socket(host, port)
        val outputStream = socket.getOutputStream()
        lazy val in = new BufferedSource(socket.getInputStream()).getLines()
        val out = new PrintWriter(new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8")))
        while(true){
          val message = readLine("Please enter message: ")
          out.println(message)
          out.flush() // send the request to the server
        }


        socket.close()
        println("Socket Closed!")
      }
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