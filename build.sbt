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
      "org.twitter4j" % "twitter4j-stream" % "4.0.4",
      "com.typesafe" % "config" % "1.3.0",
      "com.typesafe.akka" %% "akka-http-experimental" % "2.4.7"
    ),
    assemblyJarName in assembly := "twilack.jar",
    assemblyMergeStrategy in assembly := {
      case "META-INF/io.netty.versions.properties" => MergeStrategy.discard
      case x => (assemblyMergeStrategy in assembly).value(x)
    }
  ).
  dependsOn(slack)

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
