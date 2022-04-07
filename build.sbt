val ZIOVersion        = "2.0.0-RC3"

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.1"

lazy val root = (project in file("."))
  .settings(
    name := "zio-airlines"
  )

libraryDependencies ++= Seq(
  "dev.zio"           %% "zio"          % ZIOVersion,
  "dev.zio"           %% "zio-test"     % ZIOVersion % "test",
  "dev.zio"           %% "zio-test-sbt" % ZIOVersion % "test",
  "org.scalactic"     %% "scalactic"    % "3.2.11",
  "org.scalatest"     %% "scalatest"    % "3.2.11"   % "test",
  "org.scalatestplus" %% "mockito-3-4"  % "3.2.10.0" % "test"
)

scalacOptions += "-language:postfixOps"

testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
