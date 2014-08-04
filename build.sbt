name := """FaceRecognitionService"""

version := "1.0-SNAPSHOT"

resolvers += "OpenIMAJ maven releases repository" at "http://maven.openimaj.org"

resolvers += "OpenIMAJ maven snapshots repository" at "http://snapshots.openimaj.org"

resolvers += "Akka Snapshot Repository" at "http://repo.akka.io/snapshots/"

libraryDependencies ++= Seq(
  "org.openimaj" % "faces" % "1.3-SNAPSHOT",
  "com.typesafe.akka" %% "akka-actor" % "2.4-SNAPSHOT",
  "mysql" % "mysql-connector-java" % "5.1.31",
  "com.typesafe.slick" %% "slick" % "2.0.2",
  "org.webjars" %% "webjars-play" % "2.3-M1",
  "org.webjars" % "bootstrap" % "2.3.1",
  "org.webjars" % "requirejs" % "2.1.11-1"
)

lazy val root = (project in file(".")).addPlugins(PlayScala)
