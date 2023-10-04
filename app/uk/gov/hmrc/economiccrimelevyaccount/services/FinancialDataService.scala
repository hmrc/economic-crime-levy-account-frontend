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
import uk.gov.hmrc.economiccrimelevyaccount.models
import uk.gov.hmrc.economiccrimelevyaccount.models.FinancialDataResponse.findLatestFinancialObligation
import uk.gov.hmrc.economiccrimelevyaccount.models._
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.PaymentStatus._
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.PaymentType.{Interest, Payment}
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels._
import uk.gov.hmrc.http.HeaderCarrier

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
              extractValue(value.chargeReferenceNumber),
              value.getPaymentType
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
      .collect(filterItemsThatAreNotCleared)
      .map { document =>
        OutstandingPayments(
          paymentDueDate = extractValue(document.paymentDueDate),
          chargeReference = if (document.getPaymentType == Interest) {
            extractValue(getPaymentReferenceNumber(documentDetails, extractValue(document.chargeReferenceNumber)))
          } else {
            extractValue(document.chargeReferenceNumber)
          },
          fyFrom = extractValue(document.lineItemDetails).flatMap(lineItem => lineItem.periodFromDate).head,
          fyTo = extractValue(document.lineItemDetails).flatMap(lineItem => lineItem.periodToDate).head,
          amount = document.documentOutstandingAmount.getOrElse(0),
          paymentStatus = getOutstandingPaymentStatus(document),
          paymentType = document.getPaymentType,
          interestChargeReference = document.getInterestChargeReference
        )
      }

    val accruingInterestOutstandingPayments = documentDetails
      .collect(filterItemsThatHaveAccruingInterest)
      .filter(document => document.interestAccruingAmount.nonEmpty)
      .map { document =>
        OutstandingPayments(
          paymentDueDate = extractValue(document.paymentDueDate),
          chargeReference = extractValue(document.chargeReferenceNumber),
          fyFrom = extractValue(document.lineItemDetails).flatMap(lineItem => lineItem.periodFromDate).head,
          fyTo = extractValue(document.lineItemDetails).flatMap(lineItem => lineItem.periodToDate).head,
          amount = extractValue(document.interestAccruingAmount),
          paymentStatus = getOutstandingPaymentStatus(document),
          paymentType = Interest,
          interestChargeReference = None
        )
      }

    val paymentsHistory = documentDetails.flatMap { details =>
      extractValue(details.lineItemDetails)
        .filter(item => item.isCleared)
        .map { item =>
          PaymentHistory(
            paymentDate = extractValue(item.clearingDate),
            chargeReference = if (details.getPaymentType == Interest) {
              extractValue(getPaymentReferenceNumber(documentDetails, extractValue(details.chargeReferenceNumber)))
            } else {
              extractValue(details.chargeReferenceNumber)
            },
            fyFrom = extractValue(item.periodFromDate),
            fyTo = extractValue(item.periodToDate),
            amount = extractValue(item.amount),
            paymentStatus = getHistoricalPaymentStatus(item, details),
            paymentDocument = extractValue(item.clearingDocument),
            paymentType = details.getPaymentType,
            refundAmount = details.refundAmount
          )
        }
    }

    FinancialViewDetails(
      outstandingPayments = outstandingPayments ++ accruingInterestOutstandingPayments,
      paymentHistory = paymentsHistory
    )
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

  private def getPaymentReferenceNumber(documentDetails: Seq[DocumentDetails], interestReferenceNumber: String) =
    documentDetails
      .filter(alignDataForPayments)
      .filter(document => extractValue(document.interestPostedChargeRef).equalsIgnoreCase(interestReferenceNumber))
      .head
      .chargeReferenceNumber

  private def filterInPayments: PartialFunction[DocumentDetails, Boolean] = {
    case x: DocumentDetails
        if extractValue(x.documentType) == NewCharge | extractValue(x.documentType) == AmendedCharge =>
      true
  }

  private def filterOutInterest: PartialFunction[DocumentDetails, Boolean] = {
    case x: DocumentDetails if extractValue(x.documentType) == InterestCharge => false
  }

  private def alignDataForPayments: PartialFunction[DocumentDetails, Boolean] =
    filterInPayments orElse filterOutInterest
  def extractValue[A](value: Option[A]): A                                    = value.getOrElse(throw new IllegalStateException())

  private def filterItemsThatAreNotCleared: PartialFunction[DocumentDetails, DocumentDetails] = {
    case x: DocumentDetails if !x.isCleared => x
  }

  private def filterItemsThatArePayment: PartialFunction[DocumentDetails, DocumentDetails] = {
    case x: DocumentDetails if x.getPaymentType == Payment => x
  }

  private def filterItemsThatHaveAccruingInterestAmount: PartialFunction[DocumentDetails, DocumentDetails] = {
    case x: DocumentDetails if x.interestAccruingAmount.nonEmpty => x
  }

  private def filterItemsThatHaveAccruingInterest: PartialFunction[DocumentDetails, DocumentDetails] =
    filterItemsThatAreNotCleared andThen
      filterItemsThatArePayment andThen
      filterItemsThatHaveAccruingInterestAmount
}
