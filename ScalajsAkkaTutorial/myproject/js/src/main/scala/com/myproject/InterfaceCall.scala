package com.myproject

import io.circe._
import io.circe.parser._
import org.scalajs.dom.ext.Ajax
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

abstract class InterfaceCall {

  def url : String
  type Input
  type Output

  def request(input : Input)(implicit decoder: Decoder[Output], encoder: Encoder[Input]) : Future[Output] =
    Ajax.post(s"http://localhost:8080/$url", encoder(input).noSpaces)
      .map(x => decode[Output](x.responseText).right.get)
}