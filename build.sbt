name := "scala-eureka-client"
organization := "kender.github.io"

scalaVersion := "2.11.5"

libraryDependencies ++= Seq(
  modules.akka("actor"),
  modules.slf4j_api,
  modules.spray("client")
)

lazy val modules = new {
  def spray(name: String) = "io.spray" %% s"spray-$name" % "1.3.2"
  def akka(name: String) = "com.typesafe.akka" %% s"akka-$name" % "2.3.9"
  def slf4j(name: String) = "org.slf4j" % s"slf4j-$name" % "1.7.10"

  lazy val spray_json = "io.spray" %%  "spray-json" % "1.3.1"

  lazy val slf4j_api = slf4j("api")
  lazy val logback = "ch.qos.logback" % "logback-classic" % "1.1.2"

  lazy val logging =
    slf4j_api :: logback :: Nil
}
