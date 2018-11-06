val Http4sVersion = "0.18.21"
val CirceVersion = "0.9.3"
val ScalaTestVersion = "3.0.5"
val LogbackVersion = "1.2.3"
val MonixVersion = "3.0.0-RC1"
val Log4sVersion = "1.6.1"

lazy val root = (project in file("."))
  .settings(
    organization := "com.example",
    name := "http4s-monix",
    version := "0.0.1-SNAPSHOT",
    scalaVersion := "2.12.7",
    scalacOptions ++= Seq("-Ypartial-unification"),
    libraryDependencies ++= Seq(
      "org.http4s" %% "http4s-blaze-client" % Http4sVersion,
      "org.http4s" %% "http4s-async-http-client" % Http4sVersion,
      "org.http4s" %% "http4s-circe" % Http4sVersion,
      "org.http4s" %% "http4s-dsl" % Http4sVersion,
      "io.circe" %% "circe-generic" % CirceVersion,
      "io.monix" %% "monix" % MonixVersion,
      "org.scalatest" %% "scalatest" % ScalaTestVersion % "test",
      "org.log4s" %% "log4s" % Log4sVersion,
      "ch.qos.logback" % "logback-classic" % LogbackVersion
    ),
    addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.7"),
    addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.2.4")
  )

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-language:higherKinds",
  "-language:postfixOps",
  "-feature",
  "-Ypartial-unification",
)
