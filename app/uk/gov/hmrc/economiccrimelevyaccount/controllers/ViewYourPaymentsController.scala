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

package uk.gov.hmrc.economiccrimelevyaccount.controllers

import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyaccount.config.AppConfig
import uk.gov.hmrc.economiccrimelevyaccount.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyaccount.models.{DocumentDetails, OpsData}
import uk.gov.hmrc.economiccrimelevyaccount.services.FinancialDataService
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.PaymentStatus.{Due, Overdue}
import uk.gov.hmrc.economiccrimelevyaccount.views.html.PaymentsView
import uk.gov.hmrc.economiccrimelevyaccount.views.html.NoPaymentsView
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ViewYourPaymentsController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  financialDataService: FinancialDataService,
  view: PaymentsView,
  noPaymentsView: NoPaymentsView,
  appConfig: AppConfig
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = authorise.async { implicit request =>
    financialDataService.getFinancialDetails.map {
      case Some(value) => Ok(view(value, appConfig.refundBaseUrl))
      case None => Ok(noPaymentsView())
    }
  }
}
