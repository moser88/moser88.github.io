import sbtcrossproject.CrossPlugin.autoImport.{crossProject, CrossType}

name := "ScalajsAkka"

version in ThisBuild := "0.1"

scalaVersion in ThisBuild := "2.11.12"

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