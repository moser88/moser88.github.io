package com.myproject

import org.scalajs.dom.console
import org.scalajs.dom.ext._
import scala.concurrent.ExecutionContext.Implicits.global
import io.circe.generic.auto._

object MyClient{
  def main(params : Array[String]) : Unit = {
    console.log("Send Request")
    Ajax.get("http://localhost:8080/hello").map{
      x =>
        console.log("Answer received:")
        console.log(x.responseText)
    }

    IntCheck.request(IntCheck.Input(intA = 1, intB = 2)).map{
      bool =>
        console.log(bool)
    }
  }

}