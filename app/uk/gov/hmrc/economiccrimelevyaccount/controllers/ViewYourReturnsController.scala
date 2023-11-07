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

import play.api.Logging
import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyaccount.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyaccount.models.{DocumentDetails, FinancialData, Fulfilled, ObligationData, ObligationDetails, Open}
import uk.gov.hmrc.economiccrimelevyaccount.services.ECLAccountService
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.ReturnStatus.{Due, Overdue, Submitted}
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.{ReturnStatus, ReturnsOverview}
import uk.gov.hmrc.economiccrimelevyaccount.views.html.{NoReturnsView, ReturnsView}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class ViewYourReturnsController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  eclAccountService: ECLAccountService,
  returnsView: ReturnsView,
  noReturnsView: NoReturnsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with BaseController
    with ErrorHandler {

  def onPageLoad: Action[AnyContent] = authorise.async { implicit request =>
    (for {
      obligationDataOption <- eclAccountService.retrieveObligationData.asResponseError
      financialDataOption  <- eclAccountService.retrieveFinancialData.asResponseError
    } yield (obligationDataOption, financialDataOption))
      .fold(
        err => Status(err.code.statusCode)(Json.toJson(err)),
        {
          case (Some(obligationData), Some(financialData)) =>
            val returns = deriveReturnsOverview(obligationData, financialData)
            Ok(returnsView(returns))
          case _                                           => Ok(noReturnsView())
        }
      )
  }

  private def deriveReturnsOverview(
    obligationData: ObligationData,
    financialData: FinancialData
  ): Seq[ReturnsOverview] =
    obligationData.obligations
      .flatMap(_.obligationDetails.sortBy(_.inboundCorrespondenceDueDate))
      .map { details =>
        val status    = resolveStatus(details)
        val reference = getChargeReference(
          status = status,
          dueDate = details.inboundCorrespondenceDueDate,
          documentDetails = financialData.documentDetails,
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
    status match {
      case Submitted =>
        documentDetails.flatMap(details =>
          details.collectFirst {
            case DocumentDetails(_, Some(chargeReferenceNumber), _, _, _, _, _, Some(lineItemDetails), _, _, _, _, _, _)
                if lineItemDetails.exists(_.periodKey.contains(periodKey)) =>
              chargeReferenceNumber
          }
        )
      case _         => None
    }

}
