lazy val commonSettings = Seq(
  version := "1.0",
  scalaVersion := "2.11.8",
  scalacOptions ++= Seq("-feature", "-deprecation", "-Xexperimental")
)

lazy val root = (project in file(".")).aggregate(app, slack)

lazy val app = (project in file("app")).
  settings(commonSettings: _*).
  settings(
    name := "twilack-app",
    libraryDependencies ++= Seq(
      "com.twitter" %% "finagle-http" % "6.35.0",
      "org.twitter4j" % "twitter4j-stream" % "4.0.4",
      "com.typesafe" % "config" % "1.3.0"
    )
  ).dependsOn(slack)

lazy val slack = (project in file("slack")).
  settings(commonSettings: _*).
  settings(
    name := "twilack-slack",
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % "2.4.7",
      "com.typesafe.play" % "play-json_2.11" % "2.5.4",
      "org.asynchttpclient" % "async-http-client" % "2.0.7"
    )
  )
