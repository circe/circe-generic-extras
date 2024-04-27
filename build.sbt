import sbtcrossproject.{ CrossType, crossProject }

val Scala212V = "2.12.18"
val Scala213V = "2.13.7"

val circeVersion = "0.14.7"
val paradiseVersion = "2.1.1"

val jawnVersion = "1.5.1"
val munitVersion = "0.7.29"
val disciplineMunitVersion = "1.0.9"

ThisBuild / tlBaseVersion := "0.14"
ThisBuild / tlCiReleaseTags := false

ThisBuild / organization := "io.circe"
ThisBuild / crossScalaVersions := List(Scala212V, Scala213V)
ThisBuild / scalaVersion := Scala213V

ThisBuild / githubWorkflowJavaVersions := Seq("8", "17").map(JavaSpec.temurin)

ThisBuild / tlCiScalafmtCheck := true

ThisBuild / githubWorkflowAddedJobs ++= Seq(
  WorkflowJob(
    id = "coverage",
    name = "Generate coverage report",
    scalas = List(Scala213V),
    steps = List(WorkflowStep.Checkout) ++ WorkflowStep.SetupJava(
      List(githubWorkflowJavaVersions.value.last)
    ) ++ githubWorkflowGeneratedCacheSteps.value ++ List(
      WorkflowStep.Sbt(List("coverage", "rootJVM/test", "coverageAggregate")),
      WorkflowStep.Use(
        UseRef.Public(
          "codecov",
          "codecov-action",
          "v2"
        ),
        params = Map(
          "flags" -> List("${{matrix.scala}}", "${{matrix.java}}").mkString(",")
        )
      )
    )
  )
)

val docMappingsApiDir = settingKey[String]("Subdirectory in site target directory for API docs")

lazy val root =
  tlCrossRootProject.aggregate(genericExtras, benchmarks)

lazy val genericExtras = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .crossType(CrossType.Pure)
  .in(file("generic-extras"))
  .settings(
    moduleName := "circe-generic-extras",
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-generic" % circeVersion,
      "io.circe" %%% "circe-literal" % circeVersion % Test,
      "io.circe" %%% "circe-testing" % circeVersion % Test,
      "org.scalameta" %%% "munit" % munitVersion % Test,
      "org.scalameta" %%% "munit-scalacheck" % munitVersion % Test,
      "org.typelevel" %%% "discipline-munit" % disciplineMunitVersion % Test,
      "org.typelevel" %% "jawn-parser" % jawnVersion % Test,
      scalaOrganization.value % "scala-compiler" % scalaVersion.value % Provided,
      scalaOrganization.value % "scala-reflect" % scalaVersion.value % Provided
    ) ++ (
      if (scalaBinaryVersion.value == "2.12") {
        Seq(
          compilerPlugin(("org.scalamacros" % "paradise" % paradiseVersion).cross(CrossVersion.patch))
        )
      } else Nil
    ),
    testFrameworks := List(new TestFramework("munit.Framework")), // Override setting so Scalatest is disabled
    docMappingsApiDir := "api",
    addMappingsToSiteDir(Compile / packageDoc / mappings, docMappingsApiDir),
    scalacOptions ++= {
      if (scalaBinaryVersion.value == "2.13") Seq("-Ymacro-annotations") else Seq.empty
    },
    coverageHighlighting := true,
    coverageEnabled := false
  )
  .jvmSettings(
    Test / fork := true,
    coverageEnabled := (
      if (scalaBinaryVersion.value == "2.12") false else coverageEnabled.value
    )
  )
  .jsSettings()
  .nativeSettings(
    tlVersionIntroduced := List("2.12", "2.13").map(_ -> "0.14.3").toMap
  )

lazy val benchmarks = project
  .in(file("benchmarks"))
  .settings(
    moduleName := "circe-generic-extras-benchmarks",
    libraryDependencies ++= List(
      "io.circe" %%% "circe-parser" % circeVersion,
      scalaOrganization.value % "scala-reflect" % scalaVersion.value
    )
  )
  .dependsOn(genericExtras.jvm)
  .enablePlugins(JmhPlugin, NoPublishPlugin)

ThisBuild / licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0"))
ThisBuild / developers := List(
  Developer("travisbrown", "Travis Brown", "travisrobertbrown@gmail.com", url("https://twitter.com/travisbrown")),
  Developer("zmccoy", "Zach McCoy", "zachabbott@gmail.com", url("https://twitter.com/zachamccoy")),
  Developer("zarthross", "Darren Gibson", "zarthross@gmail.com", url("https://twitter.com/zarthross"))
)
