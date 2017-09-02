lazy val root = (project in file(".")).
  settings(
    name := "twilack",
    scalaVersion := "2.12.3",
    libraryDependencies ++= Seq(
      "org.asynchttpclient" % "async-http-client" % "2.0.34",
      "org.twitter4j" % "twitter4j-stream" % "4.0.4",
      "com.typesafe" % "config" % "1.3.1",
      "com.typesafe.play" %% "play-json" % "2.6.3"
    ),
    assemblyJarName in assembly := "twilack.jar",
    assemblyMergeStrategy in assembly := {
      case "META-INF/io.netty.versions.properties" => MergeStrategy.concat
      case x =>
        val strategy = (assemblyMergeStrategy in assembly).value
        strategy(x)
    }
  )
