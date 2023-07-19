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
import uk.gov.hmrc.economiccrimelevyaccount.models.{DocumentDetails, FinancialDataErrorResponse, FinancialDataResponse, FinancialDetails, NewCharge}
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.PaymentStatus._
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels._
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FinancialDataService @Inject() (
  financialDataConnector: FinancialDataConnector
)(implicit ec: ExecutionContext) {

  private val dueMonth          = 9
  private val dueDay            = 30
  private val numOfCharsForYear = 4

  def retrieveFinancialData(implicit
    hc: HeaderCarrier
  ): Future[Either[FinancialDataErrorResponse, FinancialDataResponse]] = financialDataConnector.getFinancialData()

  def getLatestFinancialObligation(financialData: FinancialDataResponse): Option[FinancialDetails] = {
    val latestObligationDetails = findLatestFinancialObligation(financialData)

    latestObligationDetails match {
      case None        => None
      case Some(value) =>
        val outstandingAmount           = extractValue(value.documentOutstandingAmount)
        val lineItemDetails             = extractValue(value.lineItemDetails)
        val firstLineItemDetailsElement = lineItemDetails.head

        Some(
          FinancialDetails(
            outstandingAmount,
            LocalDate.parse(extractValue(firstLineItemDetailsElement.periodFromDate)),
            LocalDate.parse(extractValue(firstLineItemDetailsElement.periodToDate)),
            extractValue(firstLineItemDetailsElement.periodKey)
          )
        )
    }
  }

  def getFinancialDetails(implicit hc: HeaderCarrier): Future[Option[FinancialViewDetails]] =
    retrieveFinancialData.map {
      case Left(_)         => None
      case Right(response) => Some(prepareFinancialDetails(response))
    }
  private def prepareFinancialDetails(response: FinancialDataResponse): FinancialViewDetails = {
    val documentDetails = extractValue(response.documentDetails)

    val outstandingPayments = documentDetails.map { details =>
      OutstandingPayments(
        paymentDueDate = calculateDueDate(extractValue(extractValue(details.lineItemDetails).head.periodToDate)),
        chargeReference = extractValue(details.chargeReferenceNumber),
        fyFrom =
          LocalDate.parse(extractValue(details.lineItemDetails).flatMap(lineItem => lineItem.periodFromDate).head),
        fyTo = LocalDate.parse(extractValue(details.lineItemDetails).flatMap(lineItem => lineItem.periodToDate).head),
        amount = extractValue(details.documentOutstandingAmount),
        paymentStatus = getPaymentStatus(details, "outstanding")
      )

    }

    val paymentsHistory = documentDetails.flatMap { details =>
      extractValue(details.lineItemDetails).map { item =>
        PaymentHistory(
          paymentDate = getPaymentDate(item.clearingDate),
          chargeReference = extractValue(details.chargeReferenceNumber),
          fyFrom = LocalDate.parse(extractValue(item.periodFromDate)),
          fyTo = LocalDate.parse(extractValue(item.periodToDate)),
          amount = extractValue(item.amount),
          paymentStatus = getPaymentStatus(details, "history")
        )
      }
    }

    FinancialViewDetails(outstandingPayments = outstandingPayments, paymentHistory = paymentsHistory)
  }

  private def getPaymentDate(clearingDate: Option[String]): Option[LocalDate] =
    clearingDate match {
      case Some(value) =>
        if (!value.equals("") || value.nonEmpty) { Some(LocalDate.parse(value)) }
        else { None }
      case None        => None
    }

  private def getPaymentStatus(documentDetails: DocumentDetails, paymentType: String): PaymentStatus = {
    val toDate: String     = extractValue(extractValue(documentDetails.lineItemDetails).head.periodToDate)
    val dueDate: LocalDate = calculateDueDate(toDate)

    if (extractValue(documentDetails.documentOutstandingAmount) == 0) {
      Paid
    } else if (dueDate.isBefore(LocalDate.now()) && paymentType.equalsIgnoreCase("outstanding")) {
      Overdue
    } else if (
      extractValue(documentDetails.documentOutstandingAmount) != 0 &&
      extractValue(documentDetails.documentClearedAmount) != 0 &&
      paymentType.equalsIgnoreCase("history")
    ) {
      PartiallyPaid
    } else {
      Due
    }
  }
  private def calculateDueDate(toDate: String): LocalDate                     =
    LocalDate.of(toDate.take(numOfCharsForYear).toInt, dueMonth, dueDay)

  private def extractValue[A](value: Option[A]): A = value.getOrElse(throw new IllegalStateException())
}
