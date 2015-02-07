name := "scala-eureka-client"
organization := "kender.github.io"
version := "0.1.0-SNAPSHOT"

scalaVersion := "2.11.5"

libraryDependencies ++= Seq(
  modules.akka("actor"),
  modules.slf4j_api,
  modules.spray_json,
  modules.spray("client"),
  modules.spray("httpx")
)

libraryDependencies ++= Seq (
  modules.scalatest,
  modules.logback,
  modules.akka("slf4j")
) map ( _ % "test" )

lazy val modules = new {
  def spray(name: String, version: String = "1.3.2") = "io.spray" %% s"spray-$name" % version
  def akka(name: String) = "com.typesafe.akka" %% s"akka-$name" % "2.3.9"
  def slf4j(name: String) = "org.slf4j" % s"slf4j-$name" % "1.7.10"

  lazy val spray_json = spray("json", "1.3.1")

  lazy val slf4j_api = slf4j("api")
  lazy val logback = "ch.qos.logback" % "logback-classic" % "1.1.2"

  lazy val scalatest = "org.scalatest" %% "scalatest" % "2.2.1"
}
