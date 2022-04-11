val ZIOVersion   = "2.0.0-RC3"
val ZHTTPVersion = "2.0.0-RC6"

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.1.2"

lazy val root = (project in file("."))
  .settings(
    name := "zio-airlines"
  )

libraryDependencies ++= Seq(
  "dev.zio"           %% "zio"          % ZIOVersion,
  "dev.zio"           %% "zio-test"     % ZIOVersion   % Test,
  "dev.zio"           %% "zio-test-sbt" % ZIOVersion   % Test,
  "io.d11"            %% "zhttp"        % ZHTTPVersion,
  "io.d11"            %% "zhttp-test"   % ZHTTPVersion % Test,
  "dev.zio"           %% "zio-json"     % "0.3.0-RC4",
  "org.scalactic"     %% "scalactic"    % "3.2.11",
  "org.scalatest"     %% "scalatest"    % "3.2.11"     % Test,
  "org.scalatestplus" %% "mockito-3-4"  % "3.2.10.0"   % Test
)

scalacOptions += "-language:postfixOps"

testFrameworks := Seq(new TestFramework("zio.test.sbt.ZTestFramework"))
