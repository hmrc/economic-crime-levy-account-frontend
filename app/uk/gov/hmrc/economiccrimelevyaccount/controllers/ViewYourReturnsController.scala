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
import play.api.libs.json.JsResult
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyaccount.connectors.{FinancialDataConnector, ObligationDataConnector}
import uk.gov.hmrc.economiccrimelevyaccount.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyaccount.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyaccount.models.{DocumentDetails, Fulfilled, ObligationData, ObligationDetails, Open}
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.ReturnStatus.{Due, Overdue, Submitted}
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.{ReturnStatus, ReturnsOverview}
import uk.gov.hmrc.economiccrimelevyaccount.views.html.{NoReturnsView, ReturnsView}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ViewYourReturnsController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  obligationDataConnector: ObligationDataConnector,
  financialDataConnector: FinancialDataConnector,
  returnsView: ReturnsView,
  noReturnsView: NoReturnsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with Logging {

  def onPageLoad: Action[AnyContent] = authorise.async { implicit request =>
    obligationDataConnector.getObligationData().flatMap {
      case Some(obligationData) =>
        assembleReturnsViewData(obligationData)
      case None                 => Future.successful(Ok(noReturnsView()))
    }
  }

  private def assembleReturnsViewData(
    obligationData: ObligationData
  )(implicit hc: HeaderCarrier, request: AuthorisedRequest[_]) =
    financialDataConnector
      .getFinancialData()
      .map {
        case None                        =>
          Ok(noReturnsView())
        case Some(financialDataResponse) =>
          val viewData = obligationData.obligations
            .flatMap(_.obligationDetails.sortBy(_.inboundCorrespondenceDueDate))
            .map { details =>
              val status    = resolveStatus(details)
              val reference = getChargeReference(
                status = status,
                dueDate = details.inboundCorrespondenceDueDate,
                documentDetails = financialDataResponse.documentDetails,
                periodKey = details.periodKey
              )
              ReturnsOverview(
                forgeFromToCaption(
                  details.inboundCorrespondenceFromDate.getYear,
                  details.inboundCorrespondenceToDate.getYear
                ),
                details.inboundCorrespondenceDueDate,
                status,
                details.periodKey,
                reference
              )
            }
          Ok(returnsView(viewData.sortBy(_.dueDate)(Ordering[LocalDate].reverse)))
      }
      .recover { case e =>
        logger.error(s"Exception thrown when assembling returns view data: ${e.getMessage}")
        InternalServerError
      }

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
        val chargeReference = extractValue(documentDetails)
          .find(details =>
            extractValue(details.lineItemDetails).exists(item => extractValue(item.periodKey) == periodKey)
          )
          .flatMap(_.chargeReferenceNumber)
        Some(extractValue(chargeReference))
      case _         => None
    }

  private def forgeFromToCaption(yearFrom: Integer, yearTo: Integer): String = s"$yearFrom-$yearTo"
  private def extractValue[A](value: Option[A]): A                           = value.getOrElse(throw new IllegalStateException())

}
