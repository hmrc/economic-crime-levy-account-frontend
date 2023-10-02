/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.economiccrimelevyaccount.config

import com.google.inject.{Inject, Singleton}
import play.api.Configuration
import play.api.mvc.RequestHeader
import uk.gov.hmrc.play.bootstrap.binders.SafeRedirectUrl
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject() (configuration: Configuration, servicesConfig: ServicesConfig) {

  val host: String    = configuration.get[String]("host")
  val appName: String = configuration.get[String]("appName")

  private val contactHost                  = configuration.get[String]("contact-frontend.host")
  private val contactFormServiceIdentifier = "economic-crime-levy-account-frontend"

  def feedbackUrl(implicit request: RequestHeader): String =
    s"$contactHost/contact/beta-feedback?service=$contactFormServiceIdentifier&backUrl=${SafeRedirectUrl(host + request.uri).encodedUrl}"

  val signInUrl: String       = configuration.get[String]("urls.signIn")
  val signOutUrl: String      = configuration.get[String]("urls.signOut")
  val claimUrl: String        = configuration.get[String]("urls.claim")
  val returnsUrl: String      = configuration.get[String]("urls.returns")
  val registrationUrl: String = configuration.get[String]("urls.registration")

  private val exitSurveyHost              = configuration.get[String]("feedback-frontend.host")
  private val exitSurveyServiceIdentifier = configuration.get[String]("feedback-frontend.serviceId")

  val exitSurveyUrl: String = s"$exitSurveyHost/feedback/$exitSurveyServiceIdentifier"

  val languageTranslationEnabled: Boolean =
    configuration.get[Boolean]("features.welsh-translation")

  val timeout: Int   = configuration.get[Int]("timeout-dialog.timeout")
  val countdown: Int = configuration.get[Int]("timeout-dialog.countdown")

  val enrolmentStoreProxyBaseUrl: String = servicesConfig.baseUrl("enrolment-store-proxy")

  val economicCrimeLevyAccountBaseUrl: String = servicesConfig.baseUrl("economic-crime-levy-account")

  val opsServiceUrl: String = servicesConfig.baseUrl("pay-api")

  val opsStartJourneyUrl =
    opsServiceUrl + servicesConfig.getString("microservice.services.pay-api.endpoints.startJourney")

  val dashboardUrl = servicesConfig.getString("urls.dashboard")

  val amendReturnsEnabled: Boolean = configuration.get[Boolean]("features.amendReturnsEnabled")

  val amendRegistrationEnabled: Boolean = configuration.get[Boolean]("features.amendRegistrationEnabled")

  val paymentsEnabled: Boolean = configuration.get[Boolean]("features.paymentsEnabled")
  val refundBaseUrl: String    = servicesConfig.getString("urls.refund")
  val disableRefund: Boolean   = !configuration.get[Boolean]("features.requestRefundEnabled")
}
