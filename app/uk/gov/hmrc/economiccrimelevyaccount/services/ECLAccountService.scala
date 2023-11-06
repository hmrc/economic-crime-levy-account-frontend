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

import cats.data.EitherT
import uk.gov.hmrc.economiccrimelevyaccount.connectors.ECLAccountConnector
import uk.gov.hmrc.economiccrimelevyaccount.models.FinancialData.findLatestFinancialObligation
import uk.gov.hmrc.economiccrimelevyaccount.models._
import uk.gov.hmrc.economiccrimelevyaccount.models.errors.ECLAccountError
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.PaymentStatus._
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.PaymentType.{Interest, Overpayment, StandardPayment}
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels._
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class ECLAccountService @Inject() (
  eclAccountConnector: ECLAccountConnector
)(implicit ec: ExecutionContext) {

  def retrieveFinancialData(implicit
    hc: HeaderCarrier
  ): EitherT[Future, ECLAccountError, Option[FinancialData]] =
    EitherT {
      eclAccountConnector.getFinancialData.map(Right(_)).recover {
        case error @ UpstreamErrorResponse(message, code, _, _)
            if UpstreamErrorResponse.Upstream5xxResponse
              .unapply(error)
              .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
          Left(ECLAccountError.BadGateway(reason = message, code = code))
        case NonFatal(thr) => Left(ECLAccountError.InternalUnexpectedError(thr.getMessage, Some(thr)))
      }
    }

  def retrieveObligationData(implicit
    hc: HeaderCarrier
  ): EitherT[Future, ECLAccountError, Option[ObligationData]] =
    EitherT {
      eclAccountConnector.getObligationData.map(Right(_)).recover {
        case error @ UpstreamErrorResponse(message, code, _, _)
            if UpstreamErrorResponse.Upstream5xxResponse
              .unapply(error)
              .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
          Left(ECLAccountError.BadGateway(reason = message, code = code))
        case NonFatal(thr) => Left(ECLAccountError.InternalUnexpectedError(thr.getMessage, Some(thr)))
      }
    }

  def getLatestFinancialObligation(financialData: FinancialData): Option[FinancialDetails] =
    findLatestFinancialObligation(financialData).map {
      case documentDetails @ DocumentDetails(
            _,
            _,
            _,
            _,
            _,
            _,
            outstandingAmount,
            Some(lineItemDetails),
            _,
            _,
            _,
            _,
            _,
            _
          ) if outstandingAmount.exists(x => x > BigDecimal(0)) =>
        val firstLineItemDetailsElement = lineItemDetails.head
        FinancialDetails(
          outstandingAmount.getOrElse(BigDecimal(0)),
          extractValue(firstLineItemDetailsElement.periodFromDate),
          extractValue(firstLineItemDetailsElement.periodToDate),
          firstLineItemDetailsElement.periodKey,
          documentDetails.chargeReferenceNumber,
          documentDetails.getPaymentType
        )
    }

  def getFinancialDetails(implicit hc: HeaderCarrier): Future[Option[FinancialViewDetails]] =
    eclAccountConnector.getFinancialData.map {
      case None           => None
      case Some(response) =>
        val preparedFinancialDetails = prepareFinancialDetails(response)
        if (preparedFinancialDetails.paymentHistory.isEmpty & preparedFinancialDetails.outstandingPayments.isEmpty) {
          None
        } else {
          Some(preparedFinancialDetails)
        }
    }

  private def prepareFinancialDetails(response: FinancialData): FinancialViewDetails = {
    val documentDetails = extractValue(response.documentDetails)

    val outstandingPayments = documentDetails
      .filter(document =>
        !document.isCleared && !document.documentType
          .contains(Payment) && !document.documentType.forall(_.isInstanceOf[Other])
      )
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

    val paymentsHistory = documentDetails
      .collect(filterOutOverPayment)
      .flatMap { details =>
        extractValue(details.lineItemDetails)
          .collect(filterOutItemsWithoutClearingReason andThen useOnlyRegularLineItemDetails)
          .filter(item => item.isCleared)
          .map { item =>
            PaymentHistory(
              paymentDate = extractValue(item.clearingDate),
              chargeReference = details.getPaymentType match {
                case Interest    =>
                  Some(
                    extractValue(
                      getPaymentReferenceNumber(documentDetails, extractValue(details.chargeReferenceNumber))
                    )
                  )
                case Overpayment => None
                case _           => Some(extractValue(details.chargeReferenceNumber))
              },
              fyFrom = if (details.getPaymentType == Overpayment) None else Some(extractValue(item.periodFromDate)),
              fyTo = if (details.getPaymentType == Overpayment) None else Some(extractValue(item.periodToDate)),
              amount = extractValue(item.amount),
              paymentStatus = getHistoricalPaymentStatus(item, details),
              paymentDocument = extractValue(item.clearingDocument),
              paymentType = details.getPaymentType,
              refundAmount = (details.documentType, details.contractObjectNumber) match {
                case (Some(NewCharge), Some(contractObjectNumber)) =>
                  response.refundAmount(contractObjectNumber).abs
                case _                                             => BigDecimal(0)
              }
            )
          }
      }

    val accruingInterestOutstandingPayments = documentDetails
      .collect(filterItemsThatHaveAccruingInterest)
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
      .collect(filterInPayments)
      .filter(document => document.interestPostedChargeRef.nonEmpty)
      .filter(document => containsString(document.interestPostedChargeRef, interestReferenceNumber))
      .head
      .chargeReferenceNumber

  private def filterInPayments: PartialFunction[DocumentDetails, DocumentDetails] = {
    case x: DocumentDetails
        if containsValue(x.documentType, NewCharge) | containsValue(x.documentType, AmendedCharge) =>
      x
  }

  private def filterItemsThatAreNotCleared: PartialFunction[DocumentDetails, DocumentDetails] = {
    case x: DocumentDetails if !x.isCleared => x
  }

  private def filterItemsThatArePayment: PartialFunction[DocumentDetails, DocumentDetails] = {
    case x: DocumentDetails if x.getPaymentType == StandardPayment => x
  }

  private def filterItemsThatHaveAccruingInterestAmount: PartialFunction[DocumentDetails, DocumentDetails] = {
    case x: DocumentDetails if x.interestAccruingAmount.nonEmpty => x
  }

  private def filterItemsThatHaveAccruingInterest: PartialFunction[DocumentDetails, DocumentDetails] =
    filterItemsThatAreNotCleared andThen
      filterItemsThatArePayment andThen
      filterItemsThatHaveAccruingInterestAmount

  private def filterOutOverPayment: PartialFunction[DocumentDetails, DocumentDetails] = {
    case x: DocumentDetails if x.getPaymentType == StandardPayment | x.getPaymentType == Interest => x
  }

  private def useOnlyRegularLineItemDetails: PartialFunction[LineItemDetails, LineItemDetails] = {
    case lineItemDetails: LineItemDetails
        if containsString(lineItemDetails.clearingReason, "automatic clearing")
          | containsString(lineItemDetails.clearingReason, "incoming payment") =>
      lineItemDetails
  }

  private def filterOutItemsWithoutClearingReason: PartialFunction[LineItemDetails, LineItemDetails] = {
    case lineItemDetails: LineItemDetails if lineItemDetails.clearingReason.nonEmpty => lineItemDetails
  }

  private def containsString(value: Option[String], expectedMatch: String) =
    value.exists(str => expectedMatch.equalsIgnoreCase(str))

  private def containsValue[T](value: Option[T], expectedMatch: T) =
    value.contains(expectedMatch)

  private def extractValue[A](value: Option[A]): A = value.getOrElse(throw new IllegalStateException())
}
