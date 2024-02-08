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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.economiccrimelevyaccount.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyaccount.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyaccount.models.{EclSubscriptionStatus, FinancialData, FinancialDetails, ObligationDetails}
import uk.gov.hmrc.economiccrimelevyaccount.services.{AuditService, EclAccountService, EclRegistrationService, EnrolmentStoreProxyService}
import uk.gov.hmrc.economiccrimelevyaccount.utils.CorrelationIdHelper
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.AccountViewModel
import uk.gov.hmrc.economiccrimelevyaccount.views.ViewUtils
import uk.gov.hmrc.economiccrimelevyaccount.views.html.{AccountView, ErrorTemplate}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AccountController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  enrolmentStoreProxyService: EnrolmentStoreProxyService,
  view: AccountView,
  eclAccountService: EclAccountService,
  auditService: AuditService,
  eclRegistrationService: EclRegistrationService
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with I18nSupport
    with BaseController
    with ErrorHandler {

  def onPageLoad: Action[AnyContent] = authorise.async { implicit request =>
    implicit val hc: HeaderCarrier = CorrelationIdHelper.getOrCreateCorrelationId(request)
    (for {
      registrationDate             <-
        enrolmentStoreProxyService.getEclRegistrationDate(request.eclReference).asResponseError
      obligationDataOption         <- eclAccountService.retrieveObligationData.asResponseError
      latestObligationDetailsOption = obligationDataOption.flatMap(_.latestObligation)
      financialDataOption          <- eclAccountService.retrieveFinancialData.asResponseError
      subscriptionStatus           <- eclRegistrationService.getSubscriptionStatus(request.eclReference).asResponseError
      response                      =
        determineResponse(registrationDate, latestObligationDetailsOption, financialDataOption, subscriptionStatus)
      _                             =
        auditService
          .auditAccountViewed(request.internalId, request.eclReference, obligationDataOption, financialDataOption)
    } yield response).fold(
      error => routeError(error),
      result => result
    )
  }

  private def determineResponse(
    registrationDate: LocalDate,
    latestObligationDetailsOption: Option[ObligationDetails],
    financialDataOption: Option[FinancialData],
    eclSubscriptionStatus: EclSubscriptionStatus
  )(implicit request: AuthorisedRequest[_]): Result = {
    val viewModel: AccountViewModel = financialDataOption match {
      case Some(financialData) =>
        AccountViewModel(
          request.eclReference.value,
          ViewUtils.formatLocalDate(registrationDate),
          latestObligationDetailsOption,
          financialData.latestFinancialObligation.flatMap(FinancialDetails.applyOptional),
          eclSubscriptionStatus
        )
      case None                =>
        AccountViewModel(
          request.eclReference.value,
          ViewUtils.formatLocalDate(registrationDate),
          latestObligationDetailsOption,
          None,
          eclSubscriptionStatus
        )
    }

    Ok(view(viewModel))
  }

}
