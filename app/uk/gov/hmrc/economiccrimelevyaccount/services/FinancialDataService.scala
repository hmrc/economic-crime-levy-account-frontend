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
import uk.gov.hmrc.economiccrimelevyaccount.models.Payment.SUCCESSFUL
import uk.gov.hmrc.economiccrimelevyaccount.models.{DocumentDetails, FinancialDataErrorResponse, FinancialDataResponse, FinancialDetails, NewCharge}
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.PaymentStatus._
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels._
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success}

class FinancialDataService @Inject() (
  financialDataConnector: FinancialDataConnector,
  opsService: OpsService
)(implicit ec: ExecutionContext) {

  private val dueMonth          = 9
  private val dueDay            = 30
  private val numOfCharsForYear = 4

  def retrieveFinancialData(implicit
    hc: HeaderCarrier
  ): Future[Either[FinancialDataErrorResponse, FinancialDataResponse]] = financialDataConnector.getFinancialData()

  def getLatestFinancialObligation(financialData: FinancialDataResponse)(implicit
    hc: HeaderCarrier
  ): Future[Option[FinancialDetails]] = {
    val latestObligationDetails = findLatestFinancialObligation(financialData)

    latestObligationDetails match {
      case None        => Future.successful(None)
      case Some(value) =>
        val outstandingAmount           = extractValue(value.documentOutstandingAmount)
        val lineItemDetails             = extractValue(value.lineItemDetails)
        val firstLineItemDetailsElement = lineItemDetails.head
        val chargeReference             = extractValue(value.chargeReferenceNumber)

        opsService
          .getTotalPaid(Left(chargeReference))
          .map(paidAmount =>
            Some(
              FinancialDetails(
                outstandingAmount,
                paidAmount,
                LocalDate.parse(extractValue(firstLineItemDetailsElement.periodFromDate)),
                LocalDate.parse(extractValue(firstLineItemDetailsElement.periodToDate)),
                extractValue(firstLineItemDetailsElement.periodKey),
                chargeReference
              )
            )
          )
    }
  }

  def getFinancialDetails(implicit hc: HeaderCarrier): Future[Option[FinancialViewDetails]] =
    retrieveFinancialData.flatMap {
      case Left(_)         => Future.successful(None)
      case Right(response) => prepareFinancialDetails(response)
    }

  private def prepareFinancialDetails(response: FinancialDataResponse)(implicit
    hc: HeaderCarrier
  ): Future[Option[FinancialViewDetails]] = {
    val documentDetails = extractValue(response.documentDetails)

    val outstandingPayments = documentDetails.map { details =>
      val chargeReference = extractValue(details.chargeReferenceNumber)
      opsService
        .getTotalPaid(Left(chargeReference))
        .map(paidAmount =>
          OutstandingPayments(
            paymentDueDate = calculateDueDate(extractValue(extractValue(details.lineItemDetails).head.periodToDate)),
            chargeReference = chargeReference,
            fyFrom =
              LocalDate.parse(extractValue(details.lineItemDetails).flatMap(lineItem => lineItem.periodFromDate).head),
            fyTo =
              LocalDate.parse(extractValue(details.lineItemDetails).flatMap(lineItem => lineItem.periodToDate).head),
            amount = extractValue(details.documentOutstandingAmount) - paidAmount,
            paymentStatus = getPaymentStatus(details, "outstanding")
          )
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

    val opsPaymentHistory = documentDetails.map { details =>
      opsService
        .getPayments(extractValue(details.chargeReferenceNumber))
        .map(opsPayments => opsPayments.filter(_.status == SUCCESSFUL))
        .map(successfulPayments =>
          successfulPayments.map(payment =>
            PaymentHistory(
              paymentDate = Some(payment.createdOn.toLocalDate),
              chargeReference = extractValue(details.chargeReferenceNumber),
              fyFrom = LocalDate.parse(extractValue(extractValue(details.lineItemDetails).head.periodFromDate)),
              fyTo = LocalDate.parse(extractValue(extractValue(details.lineItemDetails).head.periodToDate)),
              amount = payment.amountInPence / 100,
              paymentStatus = getPaymentStatus(details, "history")
            )
          )
        )
    }

    Future.sequence(outstandingPayments).flatMap { details =>
      Future.sequence(opsPaymentHistory).map { opsHistory =>
        Some(
          FinancialViewDetails(
            outstandingPayments = details,
            paymentHistory = paymentsHistory ++ opsHistory.flatten
          )
        )
      }
    }
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
      documentDetails.documentClearedAmount.getOrElse(1) != 0 &&
      paymentType.equalsIgnoreCase("history")
    ) {
      PartiallyPaid
    } else {
      Due
    }
  }
  private def calculateDueDate(toDate: String): LocalDate                     =
    LocalDate.of(toDate.take(numOfCharsForYear).toInt, dueMonth, dueDay)

  def extractValue[A](value: Option[A]): A = value.getOrElse(throw new IllegalStateException())
}
