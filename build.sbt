name := "twilack"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "com.twitter" %% "finagle-http" % "6.35.0",
  "org.twitter4j" % "twitter4j-stream" % "4.0.4",
  "com.typesafe" % "config" % "1.3.0",
  "com.github.gilbertw1" %% "slack-scala-client" % "0.1.4"
)

scalacOptions += "-deprecation"
