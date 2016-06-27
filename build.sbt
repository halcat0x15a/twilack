lazy val commonSettings = Seq(
  version := "1.0",
  scalaVersion := "2.11.8",
  scalacOptions += "-deprecation"
)

lazy val root = (project in file(".")).aggregate(app, slack)

lazy val app = (project in file("app")).
  settings(commonSettings: _*).
  settings(
    name := "twilack-app",
    libraryDependencies ++= Seq(
      "org.twitter4j" % "twitter4j-stream" % "4.0.4",
      "com.typesafe" % "config" % "1.3.0",
      "com.github.gilbertw1" %% "slack-scala-client" % "0.1.4"
    )
  ).dependsOn(slack)

lazy val slack = (project in file("slack")).
  settings(commonSettings: _*).
  settings(
    name := "twilack-slack",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.4.7",
      "io.netty" % "netty-handler" % "4.1.1.Final",
      "io.netty" % "netty-codec-http" % "4.1.1.Final",
      "org.json4s" %% "json4s-jackson" % "3.4.0",
      "org.asynchttpclient" % "async-http-client" % "2.0.7"
    )
  )
