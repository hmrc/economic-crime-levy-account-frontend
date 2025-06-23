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

package uk.gov.hmrc.economiccrimelevyaccount.models

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.PaymentType
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.PaymentType.{Interest, Overpayment, StandardPayment, Unknown}

import java.time.LocalDate

case class DocumentDetails(
  documentType: Option[FinancialDataDocumentType],
  chargeReferenceNumber: Option[String],
  postingDate: Option[String],
  issueDate: Option[String],
  documentTotalAmount: Option[BigDecimal],
  documentClearedAmount: Option[BigDecimal],
  documentOutstandingAmount: Option[BigDecimal],
  lineItemDetails: Option[Seq[LineItemDetails]],
  interestPostedAmount: Option[BigDecimal],
  interestAccruingAmount: Option[BigDecimal],
  interestPostedChargeRef: Option[String],
  penaltyTotals: Option[Seq[PenaltyTotals]],
  contractObjectNumber: Option[String],
  contractObjectType: Option[String]
) {

  def isType(docType: FinancialDataDocumentType): Boolean = documentType.contains(docType)

  val fyFrom: Option[LocalDate] = lineItemDetails.flatMap(_.flatMap(lineItem => lineItem.periodFromDate).headOption)

  val fyTo: Option[LocalDate] = lineItemDetails.flatMap(_.flatMap(lineItem => lineItem.periodToDate).headOption)

  val paymentDueDate: Option[LocalDate] = newestLineItem() match {
    case Some(lineItem) =>
      lineItem.periodToDate match {
        case Some(periodToDate) =>
          val dueMonth = 9
          val dueDay   = 30

          Some(
            periodToDate
              .withMonth(dueMonth)
              .withDayOfMonth(dueDay)
          )
        case None               => None
      }
    case None           => None
  }

  private val hasClearedAmount: Boolean = documentClearedAmount match {
    case Some(amount) => amount > 0
    case None         => false
  }

  val isCleared: Boolean = documentOutstandingAmount match {
    case Some(amount) => amount == 0
    case None         => true
  }

  val isOverdue: Boolean = !isCleared && (paymentDueDate match {
    case Some(date) => date.isBefore(LocalDate.now())
    case None       => false
  })

  val isPartiallyPaid: Boolean = documentOutstandingAmount match {
    case Some(amount) => amount > 0 && hasClearedAmount
    case None         => false
  }

  def isNewestLineItem(lineItem: LineItemDetails): Boolean =
    newestLineItem() match {
      case Some(newest) => newest.clearingDocument == lineItem.clearingDocument
      case None         => false
    }

  private def newestLineItem(): Option[LineItemDetails] = lineItemDetails match {
    case Some(lineItems) =>
      implicit val lineItemDetailsOrdering: Ordering[LineItemDetails] = Ordering.by { l: LineItemDetails =>
        (l.clearingDate, l.clearingDocument)
      }
      lineItems.sorted.reverse.headOption
    case None            => None
  }

  def paymentType: PaymentType =
    documentType match {
      case None        => Unknown
      case Some(value) =>
        value match {
          case NewCharge | AmendedCharge => StandardPayment
          case InterestCharge            => Interest
          case Payment                   => Overpayment
        }
    }

  def getInterestChargeReference: Option[String] =
    documentType match {
      case None        => None
      case Some(value) =>
        value match {
          case InterestCharge => chargeReferenceNumber
          case _              => None
        }
    }
}
object DocumentDetails {
  implicit val format: OFormat[DocumentDetails] = Json.format[DocumentDetails]

  private val eclMainId = "6220"
  private val eclSubId  = "3410"

  private val eclInterestMainId = "6225"
  private val eclInterestSubId  = "3415"

  private val paymentsOnAccountMainId = "0060"
  private val paymentsOnAccountSubId  = "0100"

  private def filterItemsThatHaveAccruingInterestAmount: PartialFunction[DocumentDetails, DocumentDetails] = {
    case x: DocumentDetails if x.interestAccruingAmount.nonEmpty => x
  }

  def filterInPayments: PartialFunction[DocumentDetails, DocumentDetails] = {
    case x: DocumentDetails
        if containsValue(x.documentType, NewCharge) | containsValue(x.documentType, AmendedCharge) =>
      x
  }

  private def filterItemsThatAreNotCleared: PartialFunction[DocumentDetails, DocumentDetails] = {
    case x: DocumentDetails if !x.isCleared => x
  }

  private def filterItemsThatArePayment: PartialFunction[DocumentDetails, DocumentDetails] = {
    case x: DocumentDetails if x.paymentType == StandardPayment => x
  }

  def filterItemsThatHaveAccruingInterest: PartialFunction[DocumentDetails, DocumentDetails] =
    filterItemsThatAreNotCleared andThen
      filterItemsThatArePayment andThen
      filterItemsThatHaveAccruingInterestAmount

  def filterOutOverPayment: PartialFunction[DocumentDetails, DocumentDetails] = {
    case x: DocumentDetails if x.paymentType == StandardPayment | x.paymentType == Interest => x
  }

  def filterOutstandingPayment: PartialFunction[DocumentDetails, DocumentDetails] = {
    case document: DocumentDetails if !document.isCleared && !document.documentType.contains(Payment) => document
  }

  def filterTransactionType: PartialFunction[DocumentDetails, DocumentDetails] = {
    case document @ DocumentDetails(
          _,
          _,
          _,
          _,
          _,
          _,
          _,
          Some(lineItems),
          _,
          _,
          _,
          _,
          _,
          _
        ) if lineItems.exists { x =>
          (x.mainTransaction.contains(eclMainId) && x.subTransaction.contains(eclSubId)) ||
          (x.mainTransaction.contains(eclInterestMainId) && x.subTransaction.contains(eclInterestSubId)) ||
          (x.mainTransaction.contains(paymentsOnAccountMainId) && x.subTransaction.contains(paymentsOnAccountSubId))
        } =>
      document
  }

  private def containsValue[T](value: Option[T], expectedMatch: T) =
    value.contains(expectedMatch)
}
