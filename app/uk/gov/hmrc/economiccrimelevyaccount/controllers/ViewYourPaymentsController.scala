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
import uk.gov.hmrc.economiccrimelevyaccount.services.{EclAccountService, EclRegistrationService}
import uk.gov.hmrc.economiccrimelevyaccount.utils.CorrelationIdHelper
import uk.gov.hmrc.economiccrimelevyaccount.views.html.{ErrorTemplate, NoPaymentsView, PaymentsView}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ViewYourPaymentsController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  financialDataService: EclAccountService,
  view: PaymentsView,
  noPaymentsView: NoPaymentsView,
  appConfig: AppConfig,
  eclRegistrationService: EclRegistrationService
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with I18nSupport
    with BaseController
    with ErrorHandler {

  def onPageLoad: Action[AnyContent] = authorise.async { implicit request =>
    implicit val hc: HeaderCarrier = CorrelationIdHelper.getOrCreateCorrelationId(request)
    (for {
      financialData      <- financialDataService.retrieveFinancialData.asResponseError
      subscriptionStatus <- eclRegistrationService.getSubscriptionStatus(request.eclReference).asResponseError
      viewModel          <-
        financialDataService.prepareViewModel(financialData, request.eclReference, subscriptionStatus).asResponseError
    } yield viewModel).fold(
      error => routeError(error),
      viewModelOption =>
        viewModelOption
          .map(viewModel =>
            if (viewModel.outstandingPayments.nonEmpty || viewModel.paymentHistory.nonEmpty)
              Ok(view(viewModel, appConfig))
            else
              Ok(noPaymentsView())
          )
          .getOrElse(Ok(noPaymentsView()))
    )
  }
}
