import sbtcrossproject.{ CrossType, crossProject }

ThisBuild / organization := "io.circe"

val compilerOptions = Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-unchecked",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen",
  "-Yrangepos"
)

val scala212 = "2.12.14"
val scala213 = "2.13.6"

val circeVersion = "0.14.1"
val paradiseVersion = "2.1.1"

val jawnVersion = "1.2.0"
val munitVersion = "0.7.28"
val disciplineMunitVersion = "1.0.9"

val previousCirceGenericExtrasVersion = "0.13.0"

def priorTo2_13(scalaVersion: String): Boolean =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, minor)) if minor < 13 => true
    case _                              => false
  }

ThisBuild / crossScalaVersions := Seq(scala212, scala213)

val baseSettings = Seq(
  scalacOptions ++= compilerOptions,
  scalacOptions ++= (
    if (priorTo2_13(scalaVersion.value))
      Seq(
        "-Xfuture",
        "-Yno-adapted-args",
        "-Ywarn-unused-import",
        "-Ypartial-unification"
      )
    else
      Seq(
        "-Ymacro-annotations",
        "-Ywarn-unused:imports"
      )
  ),
  Compile / console / scalacOptions ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Ywarn-unused:imports"))
  },
  Test / console / scalacOptions ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Ywarn-unused:imports"))
  },
  coverageHighlighting := true,
  libraryDependencies ++= Seq(
    scalaOrganization.value % "scala-compiler" % scalaVersion.value % Provided,
    scalaOrganization.value % "scala-reflect" % scalaVersion.value % Provided
  ) ++ (
    if (priorTo2_13(scalaVersion.value)) {
      Seq(
        compilerPlugin(("org.scalamacros" % "paradise" % paradiseVersion).cross(CrossVersion.patch))
      )
    } else Nil
  ),
  coverageEnabled := (
    if (priorTo2_13(scalaVersion.value)) false else coverageEnabled.value
  )
)

val allSettings = baseSettings ++ publishSettings

val docMappingsApiDir = settingKey[String]("Subdirectory in site target directory for API docs")

lazy val root =
  project.in(file(".")).settings(allSettings).settings(noPublishSettings).aggregate(genericExtrasJVM, genericExtrasJS)

lazy val genericExtras = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .crossType(CrossType.Pure)
  .in(file("generic-extras"))
  .settings(allSettings)
  .settings(
    moduleName := "circe-generic-extras",
    mimaPreviousArtifacts := Set("io.circe" %% "circe-generic-extras" % previousCirceGenericExtrasVersion),
    libraryDependencies ++= Seq(
      "io.circe" %%% "circe-generic" % circeVersion,
      "io.circe" %%% "circe-literal" % circeVersion % Test,
      "io.circe" %%% "circe-testing" % circeVersion % Test,
      "org.scalameta" %%% "munit" % munitVersion % Test,
      "org.scalameta" %%% "munit-scalacheck" % munitVersion % Test,
      "org.typelevel" %%% "discipline-munit" % disciplineMunitVersion % Test,
      "org.typelevel" %% "jawn-parser" % jawnVersion % Test
    ),
    testFrameworks := List(new TestFramework("munit.Framework")), // Override setting so Scalatest is disabled
    scalaJSLinkerConfig in Test ~= (_.withModuleKind(ModuleKind.CommonJSModule)),
    ghpagesNoJekyll := true,
    docMappingsApiDir := "api",
    addMappingsToSiteDir(Compile / packageDoc / mappings, docMappingsApiDir)
  )
  .jvmSettings(Test / fork := true)
  .jsSettings(coverageEnabled := false)

lazy val genericExtrasJVM = genericExtras.jvm
lazy val genericExtrasJS = genericExtras.js

lazy val publishSettings = Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseVcsSign := true,
  homepage := Some(url("https://github.com/circe/circe-generic-extras")),
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  publishMavenStyle := true,
  Test / publishArtifact := false,
  pomIncludeRepository := { _ =>
    false
  },
  publishTo := {
    val nexus = "https://oss.sonatype.org/"
    if (isSnapshot.value)
      Some("snapshots".at(nexus + "content/repositories/snapshots"))
    else
      Some("releases".at(nexus + "service/local/staging/deploy/maven2"))
  },
  autoAPIMappings := true,
  apiURL := Some(url("https://circe.github.io/circe-generic-extras/api/")),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/circe/circe-generic-extras"),
      "scm:git:git@github.com:circe/circe-generic-extras.git"
    )
  ),
  developers := List(
    Developer(
      "travisbrown",
      "Travis Brown",
      "travisrobertbrown@gmail.com",
      url("https://twitter.com/travisbrown")
    )
  )
)

lazy val noPublishSettings = Seq(
  publish := {},
  publishLocal := {},
  publishArtifact := false
)

credentials ++= (
  for {
    username <- Option(System.getenv().get("SONATYPE_USERNAME"))
    password <- Option(System.getenv().get("SONATYPE_PASSWORD"))
  } yield Credentials(
    "Sonatype Nexus Repository Manager",
    "oss.sonatype.org",
    username,
    password
  )
).toSeq

ThisBuild / githubWorkflowJavaVersions := Seq("adopt@1.8")
// No auto-publish atm. Remove this line to generate publish stage
ThisBuild / githubWorkflowPublishTargetBranches := Seq.empty
ThisBuild / githubWorkflowBuild := Seq(
  WorkflowStep.Sbt(
    List("clean", "coverage", "genericExtras/test", "coverageReport", "scalafmtCheckAll"),
    id = None,
    name = Some("Test JVM")
  ),
  WorkflowStep.Use(
    UseRef.Public("codecov", "codecov-action", "e156083f13aff6830c92fc5faa23505779fbf649"), // v1.2.1
    name = Some("Upload code coverage")
  ),
  WorkflowStep.Sbt(
    List("genericExtrasJS/test"),
    id = None,
    name = Some("Test JS")
  )
)
