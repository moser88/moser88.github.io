package com.myproject

object IntCheck extends InterfaceCall {
  case class Input(intA : Int, intB : Int)
  type Output = Boolean
  def url = "testInt"
}