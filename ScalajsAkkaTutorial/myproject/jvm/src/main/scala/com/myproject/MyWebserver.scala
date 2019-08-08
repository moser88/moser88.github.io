package com.myproject

import akka.http.scaladsl.server.{HttpApp, Route}
import io.circe.generic.auto._


object MyWebserver extends HttpApp {

  override def routes: Route =  IntCheck.route{ input =>
    input.intA > input.intB
  } ~ path("hello"){
    get {
      complete(s"Hello World")
    }
  } ~
  path ("test.html") {
    get {
      getFromFile("myproject/test.html")
    }
  } ~
  path("myproject-fastopt-bundle.js") {
    get {
      getFromFile("myproject/js/target/scala-2.11/scalajs-bundler/main/myproject-fastopt-bundle.js")
    }
  }

  def main(args: Array[String]): Unit = {
    MyWebserver.startServer("localhost", 8080)
  }
}