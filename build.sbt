scalaVersion := "2.12.4"
enablePlugins(TutPlugin)

scalacOptions ++= Seq(
  "-feature",
  "-Ypartial-unification",
  "-language:higherKinds"
)
