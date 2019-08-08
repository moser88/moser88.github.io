# Reducing Boilerplate in front-/backend-development using Scala/Scalajs/Akka

In this tutorial we develop a interface structure using scala/scalajs together with Akka and circe which will be flexible, type safe and nearly boiler
plate free. The sources can be found in this directory.

Websites and Webservices can cover a whole variety of use cases. They can be used in a small company-internal setting to monitor and interact
with data but also on a big scale offering functionallity to millions of users. The use case, for which we used the techniques I will discuss in this
tutorial, was to write a webinterface which allows to interact with an existent complex software written in scala. Nevertheless the methods will
be useful for all kind of situations.

## Requirements

### Scala
Scala offers unique features with its functional object oriented approach which allow abstraction beyond other programming languages like java.
I will assume in the following that the reader is familiar to scala. This tutorial is based on scala-2.11 but I assume that there is litte to no change
for other scala versions.

### Scalajs
Scalajs is a method of using the scala compiler to generate javascript code. In particular we will use scalajs-0.6.27 but there are release candidates for version 1.0 out there. 
The main advantage of scalajs in this setting is that it allows us to actually create frontend and backend applications
in the same language. Additionally we get all the rich scala compile time features as type safety and macros.

I will not discuss the usage of scalajs in detail but I can recommend looking at scalajs-react as a possible choice for a framework. It is a well
written library which allows us to conveniently use facebook's react from scalajs. 

### Akka
Akka is the http server library of our choice but without much effort any other choice will work too. Its syntax is nice to read, easy to define and
makes nice use of scalas features. 

### Circe
Circe is a handy library for json encoding/decoding which works heavily with macros which has the advantage that most issues are dealt 
with at compile time. Additionally it is very fast at runtime but in some situations the computational costs can be high.

## The Basic System Layout
The common structure of a modern website is an interactive javascript frontend which sends asynchronious requests to a backend server which does
the more complicated work and which is mostly a gateway to a database. The downside of this layout is that we have to design two different components
which still have to work together smoothly. Having different components always increases the risk for additional errors in the interface layers which
need to be checked using ellaborate testing.

In the following we will develop a cross project (scala/scalajs) which allows us to define the interface between the frontend and backend with compile
time checks and type safety. 

## Setting up the project with sbt
To activate scalajs together with the scalajs-bundler and crossproject one has to include the following Plugins to *project/plugins.sbt*

```
addSbtPlugin("org.scala-js" % "sbt-scalajs" % "0.6.27")
addSbtPlugin("ch.epfl.scala" % "sbt-scalajs-bundler" % "0.15.0-0.6")
addSbtPlugin("org.portable-scala" % "sbt-scalajs-crossproject" % "0.6.1")
```

Here I still use the 0.6.* version of scalajs but you can proceed to use one of the release candidates of scalajs 1.0.

The first plugin *sbt-scalajs* allows us to define scalajs projects which will compile the result to a javascript file. This javascript file 
is not "fat" but only contains the code written. The scalajs-bundler plugin uses *npm* to combine it with all depending libraries to a bundle file. Last
the sbt-scalajs-crossproject plugin allows us to define projects which are to one part scala on a JVM and to one part scalajs. 

In the build.sbt file we define the cross project "myproject" by

```
import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

lazy val myproject = crossProject(JSPlatform, JVMPlatform)
  .crossType(CrossType.Full)
  .settings(
    name := "myproject"
  )
  .jvmSettings(
    unmanagedSourceDirectories in Compile ++= Seq(
      baseDirectory.value / "src" / "main" / "scala"
    ),
    unmanagedResources := Seq(
      baseDirectory.value / "src" / "main" / "resources"
    ),
    scalaSource := baseDirectory.value / "src" / "main" / "scala",
    libraryDependencies ++= Seq(
        "com.softwaremill.sttp" %% "akka-http-backend" % "1.5.8",
        "com.typesafe.akka" %% "akka-stream" % "2.5.20",
        "org.scalaz" %% "scalaz-core" % "7.2.27",
        "io.circe" %%% "circe-core" % "0.11.1",
        "io.circe" %%% "circe-generic" % "0.11.1",
        "io.circe" %%% "circe-parser" % "0.11.1"
    )
  )
  .jsSettings(
    libraryDependencies ++=
      Seq(
        "org.scala-js" %%% "scalajs-dom" % "0.9.7",
        "org.scalaz" %%% "scalaz-core" % "7.2.27",
        "io.circe" %%% "circe-core" % "0.11.1",
        "io.circe" %%% "circe-generic" % "0.11.1",
        "io.circe" %%% "circe-parser" % "0.11.1"
      ),
      scalaJSUseMainModuleInitializer := true,
	  webpackBundlingMode := BundlingMode.Application
	  // Sometimes the bundler plugin creates problems when not supplied with a config file. Sometimes
	  //  it creates problems when a config file is supplied
      //,webpackConfigFile := Some(baseDirectory.value / "my.custom.webpack.config.js")
  ).enablePlugins(ScalaJSPlugin)
  .enablePlugins(ScalaJSBundlerPlugin)
```

If you are familiar with projects defined by sbt the structure should be not surprising to you. We have the option of supplying settings 
for all projects, for the Jvm Project and for the Js project. In particluar these projects have different dependencies even though some look quite similar.
We should always be aware that code which is shared between these two projects needs to be compileable in both subprojects.

The subprojects are then defined as "myprojectJVM" and "myprojectJS". We use the cross type CrossType.Full which defines the structure to have three
seperate source folder "/jvm", "/js" and "/shared" which should be self explanatory.

The two important options for the JS subproject are *scalaJSUseMainModuleInitializer* which determines that the bundler looks for a main method and
export this one then to be executed immediately and the definition ** that we actually deal with an Application. The *webpackConfigFile* links to a file which contains the configs for the bundler. The bundler
gave me troubles of not finding the right javascript file if I did not provide it in the config file named "my.custom.webpack.config.js" in the js subfolder.

```
module.exports = {
  "entry": {
    "myproject-fastopt": [__dirname + "/../../myproject-fastopt.js"]
  },
  "output": {
    "path": __dirname,
    "filename": "[name]-bundle.js"
  },
  "devtool": "source-map",
  "module": {
    "rules": [{
      "test": new RegExp("\\.js$"),
      "enforce": "pre",
      "use": ["source-map-loader"]
    }]
  }
}
```

It is important that the output file has the format "[name]-bundle.js" as otherwise the bundle task will end with an error.

## Basic Http server

We create a package "com.myproject" in the "/jvm/src/main/scala/" folder and define a basic Akka Webserver in the file "MyWebserver.scala"

```
package com.myproject

import akka.http.scaladsl.server.{HttpApp, Route}

object MyWebserver extends HttpApp {

  override def routes: Route = path("hello"){
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
```

This basic template will be our starting point to demonstrate efficient interaction with the scalajs client. The Webserver does two things. For once
it supplies a webservice "hello" which returns "Hello World". Further it grants access to two files. One of them will be the javascript file we create
with the scalajs plugin. The other will be a file named "test.html" we will define in the
next section.

## Basic Request from scala-js
Let us write a simple demo program which contacts the Server and prints the answer into the console. For this we create the package com.myproject
in "/js/src/main/scala" and in it a object MyClient:

```
package com.myproject

import org.scalajs.dom.console
import org.scalajs.dom.ext._
import scala.concurrent.ExecutionContext.Implicits.global

object MyClient{
  def main(params : Array[String]) : Unit = {
    console.log("Send Request")
    Ajax.get("http://localhost:8080/hello").map{
      x =>
        console.log(x.responseText)
    }
  }

}
```

In the above code we send a request to our local server (which needs be started for this to work) and print the answer. This code will get translated to
javascript if we execute
```
myproject/compile:fastOptJS::webpack
```
in the sbt shell. In "target/scala-2.11/scalajs-bundler/main/myproject-fastopt-bundle.js" the resulting javascript file should be found. If you run
into troubles try to clean and reload as this solves most of the problems.

To be able to test the javascript file we need to create additionally a html site we name "test.html" in the myproject folder.
```
<!doctype html>
<html>
<body>
<!-- Include Scala.js compiled code -->
<script type="text/javascript" src="http://localhost:8080/myproject-fastopt-bundle.js"></script>
</body>
</html>
```
If we start the server and open this website in a browser then we should see "Hello World" appearing on the console.

# Generalization

In this section we will use scala features to make the development of the interface as easy as possible. For this we will define the class InterfaceCall and
we have to define it seperately in the js and the jvm part. 

## JVM Part
The basic features of the InterfaceCall will be input data which will be sent to the server and output data which will be sent back. Additionally we need
to provide the url where to find the service. On the JVM we need to define the route we need to add to the Akka Server and it will take a function which maps
the input parameters to the output parameters.

```
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
```

We see that in this trait the types for input and output are not set at all. In most cases they will be defined to be some instances of
case classes as this allows us to build the Decoder and Encoder we need for circe automatically. The parent traits of InterfaceCall are necessary as 
they provide implicit functions needed in the route function. 

The route function allows us to define a Route by supplying a function which maps Input to Output. There are many advantages to this. It forces
the user of this code to respect the Input/Output pattern and hides the whole json encoding/decoding from the actual logic. 

The class we defined is not ideal in the sense of error handling. It would make sense to pass the Either direcly to the client.

## JS Part
Of course the scalajs part must be compatible but instead of supplying a route method which defined the server side we will supply a request method
which implements a client side request:

```
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
```

Again I do not focus here on proper error handling and especially the right.get is not a good way of dealing with errors. Further it is not a good
practice to hard code the server url. This could be set using a global config var in the test.html javascript file.

## Defining a endpoint using InterfaceCall

As we defined InterfaceCall for both the jvm as also scalajs we can now use InterfaceCall in the shared source. There we can create a new class named
IntCheck.

```
package com.myproject

object IntCheck extends InterfaceCall {
  case class Input(intA : Int, intB : Int)
  type Output = Boolean
  def url = "testInt"
}
```

This is all which is necessary in the shared code to define the interface. We see how freely we can choose the types of Input and Output. One as a
case class and one as a Boolean. Basically we can choose everything for which we can supply circe Encoder/Decoder. 

Next step is to implement the logic. This has to be done - in general - in the JVM code as we most likely want to use features which are not available on
the client side. In the MyWebserver class we extend the routes property to

```
override def routes: Route = IntCheck.route{ input =>
  input.intA > input.intB
} ~ 
...
```

and by adding the import 
```
import io.circe.generic.auto._
```
to create the Encoder and Decoder.

We nicely observe that there is basically no boilerplate left. In particular we don't have to explicitly write a path nor the json parsing.

On the client side we can add the following code to the main method in MyClient to test the request

```
IntCheck.request(IntCheck.Input(intA = 1, intB = 2)).map{
  bool =>
    console.log(bool)
}
```
and also as on the server side we need to include 
```import io.circe.generic.auto._```
to generate the JSON encoder/decoder.

If we run this code we should see a "false" appearing in the console. 

We see that all relevant information for the interface is defined in the shared code. Hence we do not have to deal with different path strings and get
parameters which are susceptable to errors. The logic on both sides can be implemented using meaningful types. By adding a request function to the jvm
InterfaceCall we can even use the same code to offer a convenient api to other jvm programs without additional work.

## Summary

In this Tutorial we constructed a flexible type safe webservice interface between a scala Akka backend server and a scalajs frontend. We saw that using 
scalas generalization features we could remove nearly all boilerplate code and avoid errors sources to focus on the important logic without loosing flexibility.

## Outlook
We looked at a simple implementation using scala abstract types to remove boilerplate in a scala/scalajs setup. Above techniques can be extended to
automatically define an interface for CRUD operations on a database table or automatic search in enum tables. Together with code generation we can get
to the point where database changes are automatically supplied as an api endpoint to the client.  

## Pitfalls

### scalajs-bundler
In my experience it is not always easy to control the scalajs-bundler. One particular issue was that it didn't work in windows because it
couldn't find "npm" as there is no "npm.exe" but just a "npm.cmd". In my case I created a "npm.exe" which forwarded to "npm.cmd" but I am sure
there must be a more ellegant solution out there.

Another issue is that the output file needs to be named "[name]-bundle.js" as every other name causes the task to end with an error. If there are
issues they can often be solved with a clean/compile combination.

### IDE issues
I use Intellij for developing and it has sometimes problems in resolving shared code if it refers to code defined twice in the JS and JVM subfolders.
A possible solution is to create a jvm project on which myproject depends and which includes the JVM code in question.  