package com.myproject

import akka.http.scaladsl.server.directives.{MarshallingDirectives, MethodDirectives, PathDirectives, RouteDirectives}
import akka.http.scaladsl.server.{PathMatcher0, Route}
import akka.http.scaladsl.unmarshalling.PredefinedFromEntityUnmarshallers
import io.circe.{Decoder, Encoder}
import io.circe.syntax._
import io.circe.parser._
import cats.syntax.either._

trait InterfaceCall extends PathDirectives with MarshallingDirectives with PredefinedFromEntityUnmarshallers with RouteDirectives with MethodDirectives {

  def url: String
  type Output
  type Input

  /**
    * Routes the data such that the Input is parsed in json and the return type is of type Output
    */
  def route(func: Input => Output)(implicit paramsDecoder: Decoder[Input], encoder: Encoder[Output]): Route = path(url.split("/").map{ x => val y : PathMatcher0 = x; y}.reduce((a, b) => a / b)) {

    post {
      entity(as[String]) { json =>
        decode[Input](json).map(func).map(_.asJson) match {
          case Right(jsonConvert) => complete(jsonConvert.noSpaces)
          case Left(error)        => complete(error.toString)
        }
      }
    }
  }

}