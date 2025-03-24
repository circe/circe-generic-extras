import sbtcrossproject.{ CrossType, crossProject }

val Scala212V = "2.12.19"
val Scala213V = "2.13.16"
val Scala3V = "3.3.5"

val circeVersion = "0.14.12"
val paradiseVersion = "2.1.1"

val jawnVersion = "1.6.0"
val munitVersion = "1.0.0"
val disciplineMunitVersion = "2.0.0"

def crossSettings[T](scalaVersion: String, if3: List[T], if2: List[T]) =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((3, _))       => if3
    case Some((2, 12 | 13)) => if2
    case _                  => Nil
  }

ThisBuild / tlBaseVersion := "0.14"
ThisBuild / tlCiReleaseTags := true
ThisBuild / tlFatalWarnings := false // we currently have a lot of warnings that will need to be fixed

ThisBuild / organization := "io.circe"
ThisBuild / scalaVersion := Scala213V
ThisBuild / crossScalaVersions := List(Scala212V, Scala213V, Scala3V)

ThisBuild / githubWorkflowJavaVersions := Seq("8", "17").map(JavaSpec.temurin)

ThisBuild / tlCiScalafmtCheck := true

ThisBuild / githubWorkflowAddedJobs ++= Seq(
  WorkflowJob(
    id = "coverage",
    name = "Generate coverage report",
    scalas = List(Scala213V, Scala3V),
    steps = List(WorkflowStep.Checkout) ++ WorkflowStep.SetupJava(
      List(githubWorkflowJavaVersions.value.last)
    ) ++ githubWorkflowGeneratedCacheSteps.value ++ List(
      WorkflowStep.Sbt(List("coverage", "rootJVM/test", "coverageAggregate")),
      WorkflowStep.Use(
        UseRef.Public(
          "codecov",
          "codecov-action",
          "v4"
        ),
        params = Map(
          "flags" -> List("${{matrix.scala}}", "${{matrix.java}}").mkString(",")
        )
      )
    )
  )
)

def do_configure(project: Project) = project.settings(
  Seq(
    Test / scalacOptions -= "-Xfatal-warnings"
  )
)

//val docMappingsApiDir = settingKey[String]("Subdirectory in site target directory for API docs")

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
      "org.typelevel" %% "jawn-parser" % jawnVersion % Test
    ) ++ (
      if (scalaBinaryVersion.value == "2.12") {
        Seq(
          compilerPlugin(("org.scalamacros" % "paradise" % paradiseVersion).cross(CrossVersion.patch))
        )
      } else Nil
    ) ++ crossSettings(
      scalaBinaryVersion.value,
      if3 = List(),
      if2 = List(
        scalaOrganization.value % "scala-compiler" % scalaVersion.value % Provided,
        scalaOrganization.value % "scala-reflect" % scalaVersion.value % Provided
      )
    ),
    testFrameworks := List(new TestFramework("munit.Framework")), // Override setting so Scalatest is disabled
    // docMappingsApiDir := "api",
    // addMappingsToSiteDir(Compile / packageDoc / mappings, docMappingsApiDir),
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
    tlVersionIntroduced := List("2.12", "2.13").map(_ -> "0.14.4").toMap
  )
  .configure(do_configure)

lazy val benchmarks = project
  .in(file("benchmarks"))
  .settings(
    moduleName := "circe-generic-extras-benchmarks",
    libraryDependencies ++= List(
      "io.circe" %%% "circe-parser" % circeVersion,
    )
  )
  .dependsOn(genericExtras.jvm)
  .enablePlugins(JmhPlugin, NoPublishPlugin)

ThisBuild / startYear := Some(2019)
ThisBuild / developers := List(
  Developer("travisbrown", "Travis Brown", "travisrobertbrown@gmail.com", url("https://twitter.com/travisbrown")),
  Developer("zmccoy", "Zach McCoy", "zachabbott@gmail.com", url("https://twitter.com/zachamccoy")),
  Developer("zarthross", "Darren Gibson", "zarthross@gmail.com", url("https://twitter.com/zarthross")),
  Developer("hamnis", "Erlend Hamnaberg", "erlend@hamnaberg.net", url("https://github.com/hamnis"))
)
