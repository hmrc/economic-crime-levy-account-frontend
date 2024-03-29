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

import play.api.i18n.{I18nSupport, Messages}
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Request, Result}
import uk.gov.hmrc.economiccrimelevyaccount.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyaccount.models._
import uk.gov.hmrc.economiccrimelevyaccount.services.{EclAccountService, EclRegistrationService}
import uk.gov.hmrc.economiccrimelevyaccount.utils.CorrelationIdHelper
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.ReturnStatus.{Due, Overdue, Submitted}
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.{ReturnStatus, ReturnsOverview, ReturnsViewModel}
import uk.gov.hmrc.economiccrimelevyaccount.views.html.{ErrorTemplate, NoReturnsView, ReturnsView}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ViewYourReturnsController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  eclAccountService: EclAccountService,
  returnsView: ReturnsView,
  noReturnsView: NoReturnsView,
  eclRegistrationService: EclRegistrationService
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with I18nSupport
    with BaseController
    with ErrorHandler {

  def onPageLoad: Action[AnyContent] = authorise.async { implicit request =>
    implicit val hc: HeaderCarrier = CorrelationIdHelper.getOrCreateCorrelationId(request)
    (for {
      obligationDataOption <- eclAccountService.retrieveObligationData.asResponseError
      financialDataOption  <- eclAccountService.retrieveFinancialData.asResponseError
      subscriptionStatus   <- eclRegistrationService.getSubscriptionStatus(request.eclReference).asResponseError
      response              = prepareResponse(obligationDataOption, financialDataOption, request.eclReference, subscriptionStatus)
    } yield response)
      .fold(
        error => routeError(error),
        response => response
      )
  }

  private def prepareResponse(
    obligationData: Option[ObligationData],
    financialData: Option[FinancialData],
    eclRegistrationReference: EclReference,
    eclSubscriptionStatus: EclSubscriptionStatus
  )(implicit request: Request[_], messages: Messages): Result =
    (obligationData, financialData) match {
      case (Some(obligationData), financialData) =>
        val returns   = deriveReturnsOverview(obligationData, financialData)
        val viewModel = ReturnsViewModel(returns, eclRegistrationReference, eclSubscriptionStatus)
        Ok(returnsView(viewModel)(request, messages))
      case _                                     => Ok(noReturnsView()(request, messages))
    }

  private def deriveReturnsOverview(
    obligationData: ObligationData,
    financialData: Option[FinancialData]
  ): Seq[ReturnsOverview] =
    obligationData.obligations
      .flatMap(_.obligationDetails.sortBy(_.inboundCorrespondenceDueDate))
      .map { details =>
        val status    = resolveStatus(details)
        val reference = getChargeReference(
          status = status,
          dueDate = details.inboundCorrespondenceDueDate,
          documentDetails = financialData.flatMap(_.documentDetails),
          periodKey = details.periodKey
        )

        val fromToCaption =
          s"${details.inboundCorrespondenceFromDate.getYear}-${details.inboundCorrespondenceToDate.getYear}"

        ReturnsOverview(
          fromToCaption,
          details.inboundCorrespondenceDueDate,
          status,
          details.periodKey,
          reference
        )
      }
      .sortBy(_.dueDate)(Ordering[LocalDate].reverse)

  private def resolveStatus(details: ObligationDetails): ReturnStatus = details.status match {
    case Open      => if (details.isOverdue) Overdue else Due
    case Fulfilled => Submitted
  }

  private def getChargeReference(
    status: ReturnStatus,
    dueDate: LocalDate,
    documentDetails: Option[Seq[DocumentDetails]],
    periodKey: String
  ): Option[String] =
    (status, documentDetails) match {
      case (Submitted, Some(details)) =>
        details.collectFirst {
          case DocumentDetails(_, Some(chargeReferenceNumber), _, _, _, _, _, Some(lineItemDetails), _, _, _, _, _, _)
              if lineItemDetails.exists(_.periodKey.contains(periodKey)) =>
            chargeReferenceNumber
        }
      case _                          => None
    }

}
