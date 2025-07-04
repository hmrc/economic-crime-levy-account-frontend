import play.sbt.routes.RoutesKeys
import scoverage.ScoverageKeys

val appName = "economic-crime-levy-account-frontend"

val silencerVersion = "1.7.7"

lazy val root = (project in file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(inConfig(Test)(testSettings): _*)
  .settings(majorVersion := 0)
  .settings(inThisBuild(buildSettings))
  .settings(scoverageSettings: _*)
  .settings(scalaCompilerOptions: _*)
  .settings(
    scalaVersion := "2.13.16",
    name := appName,
    RoutesKeys.routesImport ++= Seq(
      "uk.gov.hmrc.economiccrimelevyaccount.models._",
      "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl"
    ),
    TwirlKeys.templateImports ++= Seq(
      "play.twirl.api.HtmlFormat",
      "play.twirl.api.HtmlFormat._",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
      "uk.gov.hmrc.hmrcfrontend.views.config._",
      "uk.gov.hmrc.economiccrimelevyaccount.views.ViewUtils._",
      "uk.gov.hmrc.economiccrimelevyaccount.models._",
      "uk.gov.hmrc.economiccrimelevyaccount.controllers.routes._",
      "uk.gov.hmrc.economiccrimelevyaccount.viewmodels.govuk.all._"
    ),
    PlayKeys.playDefaultPort := 14008,
    libraryDependencies ++= AppDependencies(),
    retrieveManaged := true,
    (update / evictionWarningOptions).withRank(KeyRanks.Invisible) :=
      EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    // concatenate js
    Concat.groups := Seq(
      "javascripts/application.js" ->
        group(
          Seq(
            "javascripts/prevent-resubmit-warning.js",
            "javascripts/print-dialogue.js"
          )
        )
    ),
  )

lazy val testSettings: Seq[Def.Setting[_]] = Seq(
  fork := true
)

lazy val buildSettings = Def.settings(
  scalafmtOnCompile := true,
  useSuperShell := false
)

val excludedScoveragePackages: Seq[String] = Seq(
  "<empty>",
  "Reverse.*",
  ".*handlers.*",
  ".*components.*",
  "uk.gov.hmrc.BuildInfo",
  "app.*",
  "prod.*",
  ".*Routes.*",
  "testOnly.*",
  "testOnlyDoNotUseInAppConf.*",
  ".*viewmodels.govuk.*",
  ".*views.html.*"
)

val scoverageSettings: Seq[Setting[_]] = Seq(
  ScoverageKeys.coverageExcludedFiles := excludedScoveragePackages.mkString(";"),
  ScoverageKeys.coverageMinimumStmtTotal := 90,
  ScoverageKeys.coverageFailOnMinimum := true,
  ScoverageKeys.coverageHighlighting := true
)

val scalaCompilerOptions: Def.Setting[Task[Seq[String]]] = scalacOptions ++= Seq(
  "-feature",
  "-deprecation",
  "-Wconf:cat=feature:ws,cat=optimizer:ws,src=target/.*:s",
  "-Xlint:-byname-implicit"
)

addCommandAlias("runAllChecks", ";clean;compile;scalafmtCheckAll;coverage;test;scalastyle;coverageReport")
