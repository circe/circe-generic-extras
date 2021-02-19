import sbtcrossproject.{ CrossType, crossProject }

organization in ThisBuild := "io.circe"

val compilerOptions = Seq(
  "-deprecation",
  "-encoding",
  "UTF-8",
  "-feature",
  "-language:existentials",
  "-language:higherKinds",
  "-unchecked",
  "-Ywarn-dead-code",
  "-Ywarn-numeric-widen"
)

val circeVersion = "0.13.0"
val paradiseVersion = "2.1.1"

val jawnVersion = "1.0.0"
val scalaTestVersion = "3.2.5"
val scalaTestPlusVersion = "3.2.2.0"

val previousCirceGenericExtrasVersion = "0.12.2"

def priorTo2_13(scalaVersion: String): Boolean =
  CrossVersion.partialVersion(scalaVersion) match {
    case Some((2, minor)) if minor < 13 => true
    case _                              => false
  }

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
  scalacOptions in (Compile, console) ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Ywarn-unused:imports"))
  },
  scalacOptions in (Test, console) ~= {
    _.filterNot(Set("-Ywarn-unused-import", "-Ywarn-unused:imports"))
  },
  coverageHighlighting := true,
  (scalastyleSources in Compile) ++= (unmanagedSourceDirectories in Compile).value,
  libraryDependencies ++= Seq(
    scalaOrganization.value % "scala-compiler" % scalaVersion.value % Provided,
    scalaOrganization.value % "scala-reflect" % scalaVersion.value % Provided
  ) ++ (
    if (priorTo2_13(scalaVersion.value)) {
      Seq(
        compilerPlugin(("org.scalamacros" % "paradise" % paradiseVersion).cross(CrossVersion.patch))
      )
    } else Nil
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
      "org.scalatest" %%% "scalatest" % scalaTestVersion % Test,
      "org.scalatestplus" %%% "scalacheck-1-14" % scalaTestPlusVersion % Test,
      "org.typelevel" %% "jawn-parser" % jawnVersion % Test
    ),
    ghpagesNoJekyll := true,
    docMappingsApiDir := "api",
    addMappingsToSiteDir(mappings in (Compile, packageDoc), docMappingsApiDir)
  )
  .jvmSettings(fork in Test := true)

lazy val genericExtrasJVM = genericExtras.jvm
lazy val genericExtrasJS = genericExtras.js

lazy val publishSettings = Seq(
  releaseCrossBuild := true,
  releasePublishArtifactsAction := PgpKeys.publishSigned.value,
  releaseVcsSign := true,
  homepage := Some(url("https://github.com/circe/circe-generic-extras")),
  licenses := Seq("Apache 2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")),
  publishMavenStyle := true,
  publishArtifact in Test := false,
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
