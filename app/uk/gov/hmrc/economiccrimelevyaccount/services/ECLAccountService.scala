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
import uk.gov.hmrc.economiccrimelevyaccount.models._
import uk.gov.hmrc.economiccrimelevyaccount.models.errors.ECLAccountError
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.PaymentStatus._
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.PaymentType.{Interest, Overpayment, StandardPayment}
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels._
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
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

  def prepareFinancialDetails(
    financialDataOption: Option[FinancialData]
  ): EitherT[Future, ECLAccountError, Option[FinancialViewDetails]] =
    Try {
      financialDataOption.map { response =>
        val documentDetails = extractValue(response.documentDetails)

        val outstandingPayments = documentDetails
          .filter(document => !document.isCleared && !document.documentType.contains(Payment))
          .collect {
            getOutstandingPayments(documentDetails)
          }

        val paymentsHistory = documentDetails
          .collect(filterOutOverPayment)
          .flatMap { details =>
            extractValue(details.lineItemDetails)
              .collect(filterOutItemsWithoutClearingReason andThen useOnlyRegularLineItemDetails)
              .filter(item => item.isCleared)
              .collect {
                getPaymentHistory(details, documentDetails, response)
              }
          }

        val accruingInterestOutstandingPayments = documentDetails
          .collect(filterItemsThatHaveAccruingInterest)
          .collect {
            getOutstandingPayments(documentDetails)
          }

        FinancialViewDetails(
          outstandingPayments = outstandingPayments ++ accruingInterestOutstandingPayments,
          paymentHistory = paymentsHistory
        )
      }
    } match {
      case Success(financialViewDetails) => EitherT.rightT[Future, ECLAccountError](financialViewDetails)
      case Failure(_)                    =>
        EitherT.leftT[Future, Option[FinancialViewDetails]](
          ECLAccountError.InternalUnexpectedError("Missing data required for FinancialViewDetails", None)
        )
    }

  private def getPaymentHistory(
    details: DocumentDetails,
    documentDetails: Seq[DocumentDetails],
    response: FinancialData
  ): PartialFunction[LineItemDetails, PaymentHistory] = {
    case item @ LineItemDetails(
          Some(amount),
          _,
          Some(clearingDate),
          Some(clearingDocument),
          _,
          _,
          _,
          _,
          _
        ) =>
      val fyFrom = if (details.paymentType == Overpayment) None else item.periodFromDate
      val fyTo   = if (details.paymentType == Overpayment) None else item.periodToDate

      val chargeReference = details.paymentType match {
        case Interest    =>
          details.chargeReferenceNumber.flatMap(value => getPaymentReferenceNumber(documentDetails, value))
        case Overpayment => None
        case _           => details.chargeReferenceNumber
      }

      val refundAmount = (details.documentType, details.contractObjectNumber) match {
        case (Some(NewCharge), Some(contractObjectNumber)) =>
          response.refundAmount(contractObjectNumber).abs
        case _                                             => BigDecimal(0)
      }

      PaymentHistory(
        paymentDate = clearingDate,
        chargeReference = chargeReference,
        fyFrom = fyFrom,
        fyTo = fyTo,
        amount = amount,
        paymentStatus = getHistoricalPaymentStatus(item, details),
        paymentDocument = clearingDocument,
        paymentType = details.paymentType,
        refundAmount = refundAmount
      )

  }
  private def getOutstandingPayments(
    documentDetails: Seq[DocumentDetails]
  ): PartialFunction[DocumentDetails, OutstandingPayments] = {
    case document
        if document.fyFrom.isDefined && document.fyTo.isDefined && document.paymentDueDate.isDefined && document
          .paymentReference(documentDetails)
          .isDefined =>
      OutstandingPayments(
        paymentDueDate = document.paymentDueDate.get,
        chargeReference = document
          .paymentReference(documentDetails)
          .get,
        fyFrom = document.fyFrom.get,
        fyTo = document.fyTo.get,
        amount = document.documentOutstandingAmount.getOrElse(0),
        paymentStatus = document.outstandingPaymentStatus,
        paymentType = document.paymentType,
        interestChargeReference = document.interestChargeReference
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

  private def getPaymentReferenceNumber(documentDetails: Seq[DocumentDetails], interestReferenceNumber: String) =
    documentDetails
      .filter(_.inPayment)
      .filter(document => containsString(document.interestPostedChargeRef, interestReferenceNumber))
      .head
      .chargeReferenceNumber

  private def filterItemsThatAreNotCleared: PartialFunction[DocumentDetails, DocumentDetails] = {
    case x: DocumentDetails if !x.isCleared => x
  }

  private def filterItemsThatArePayment: PartialFunction[DocumentDetails, DocumentDetails] = {
    case x: DocumentDetails if x.paymentType == StandardPayment => x
  }

  private def filterItemsThatHaveAccruingInterestAmount: PartialFunction[DocumentDetails, DocumentDetails] = {
    case x: DocumentDetails if x.interestAccruingAmount.nonEmpty => x
  }

  private def filterItemsThatHaveAccruingInterest: PartialFunction[DocumentDetails, DocumentDetails] =
    filterItemsThatAreNotCleared andThen
      filterItemsThatArePayment andThen
      filterItemsThatHaveAccruingInterestAmount

  private def filterOutOverPayment: PartialFunction[DocumentDetails, DocumentDetails] = {
    case x: DocumentDetails if x.paymentType == StandardPayment | x.paymentType == Interest => x
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

  private def extractValue[A](value: Option[A]): A = value.getOrElse(throw new IllegalStateException())
}
