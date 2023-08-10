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

package uk.gov.hmrc.economiccrimelevyaccount.services

import uk.gov.hmrc.economiccrimelevyaccount.connectors.FinancialDataConnector
import uk.gov.hmrc.economiccrimelevyaccount.models.FinancialDataResponse.findLatestFinancialObligation
import uk.gov.hmrc.economiccrimelevyaccount.models.{DocumentDetails, FinancialDataErrorResponse, FinancialDataResponse, FinancialDetails, LineItemDetails}
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.PaymentStatus._
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels._
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FinancialDataService @Inject() (
  financialDataConnector: FinancialDataConnector
)(implicit ec: ExecutionContext) {

  def retrieveFinancialData(implicit
    hc: HeaderCarrier
  ): Future[Either[FinancialDataErrorResponse, FinancialDataResponse]] = financialDataConnector.getFinancialData()

  def getLatestFinancialObligation(financialData: FinancialDataResponse): Option[FinancialDetails] = {
    val latestObligationDetails = findLatestFinancialObligation(financialData)

    latestObligationDetails match {
      case None        => None
      case Some(value) =>
        val outstandingAmount           = value.documentOutstandingAmount.getOrElse(BigDecimal(0))
        val lineItemDetails             = extractValue(value.lineItemDetails)
        val firstLineItemDetailsElement = lineItemDetails.head

        if (outstandingAmount > 0) {
          Some(
            FinancialDetails(
              outstandingAmount,
              extractValue(firstLineItemDetailsElement.periodFromDate),
              extractValue(firstLineItemDetailsElement.periodToDate),
              extractValue(firstLineItemDetailsElement.periodKey),
              extractValue(value.chargeReferenceNumber)
            )
          )
        } else {
          None
        }
    }
  }

  def getFinancialDetails(implicit hc: HeaderCarrier): Future[Option[FinancialViewDetails]] =
    retrieveFinancialData.map {
      case Left(_)         => None
      case Right(response) => Some(prepareFinancialDetails(response))
    }

  private def prepareFinancialDetails(response: FinancialDataResponse): FinancialViewDetails = {
    val documentDetails = extractValue(response.documentDetails)

    val outstandingPayments = documentDetails
      .filter(document => !document.isCleared)
      .map { document =>
        OutstandingPayments(
          paymentDueDate = extractValue(document.paymentDueDate),
          chargeReference = extractValue(document.chargeReferenceNumber),
          fyFrom = extractValue(document.lineItemDetails).flatMap(lineItem => lineItem.periodFromDate).head,
          fyTo = extractValue(document.lineItemDetails).flatMap(lineItem => lineItem.periodToDate).head,
          amount = document.documentOutstandingAmount.getOrElse(0),
          paymentStatus = getOutstandingPaymentStatus(document)
        )
      }

    val paymentsHistory = documentDetails.flatMap { details =>
      extractValue(details.lineItemDetails)
        .filter(item => item.isCleared)
        .map { item =>
          PaymentHistory(
            paymentDate = extractValue(item.clearingDate),
            chargeReference = extractValue(details.chargeReferenceNumber),
            fyFrom = extractValue(item.periodFromDate),
            fyTo = extractValue(item.periodToDate),
            amount = extractValue(item.amount),
            paymentStatus = getHistoricalPaymentStatus(item, details),
            paymentDocument = extractValue(item.clearingDocument)
          )
        }
    }

    FinancialViewDetails(outstandingPayments = outstandingPayments, paymentHistory = paymentsHistory)
  }

  private def getHistoricalPaymentStatus(lineItem: LineItemDetails, document: DocumentDetails): PaymentStatus =
    if (document.isCleared) {
      if (document.isNewestLineItem(lineItem)) Paid else PartiallyPaid
    } else if (document.isPartiallyPaid) {
      PartiallyPaid
    } else {
      Due
    }

  private def getOutstandingPaymentStatus(document: DocumentDetails): PaymentStatus =
    if (document.isOverdue) {
      Overdue
    } else {
      Due
    }

  def extractValue[A](value: Option[A]): A = value.getOrElse(throw new IllegalStateException())
}
