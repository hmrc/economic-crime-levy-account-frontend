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

import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json._
import uk.gov.hmrc.economiccrimelevyaccount.models.FinancialData.extractValue
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.PaymentType
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.PaymentType._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}

import java.time.LocalDate

case class FinancialData(totalisation: Option[Totalisation], documentDetails: Option[Seq[DocumentDetails]]) {
  private def getDocumentsByContractObject(
    contractObjectNumber: String,
    contractObjectType: String
  ): Seq[DocumentDetails] =
    extractValue(documentDetails).filter(document =>
      document.contractObjectType.contains(contractObjectType)
        && document.contractObjectNumber.contains(contractObjectNumber)
    )

  def refundAmount(contractObjectNumber: String): BigDecimal =
    getDocumentsByContractObject(contractObjectNumber, "ECL")
      .collect(outOverPaymentPredicate)
      .flatMap(_.documentTotalAmount)
      .sum

  def outOverPaymentPredicate: PartialFunction[DocumentDetails, DocumentDetails] = {
    case document: DocumentDetails if document.getPaymentType == Overpayment => document
  }

  val latestFinancialObligationOption: Option[DocumentDetails] =
    documentDetails
      .map(_.collect {
        case docDetails
            if (docDetails.documentType.contains(NewCharge)
              || docDetails.documentType.contains(AmendedCharge)
              || docDetails.documentType.contains(InterestCharge)) && !docDetails.isCleared =>
          docDetails
      })
      .flatMap(_.sortBy(_.postingDate).headOption)

}

object FinancialData {

  implicit val format: OFormat[FinancialData] = Json.format[FinancialData]

  private def extractValue[A](value: Option[A]): A = value.getOrElse(throw new IllegalStateException())
}

case class Totalisation(
  totalAccountBalance: Option[BigDecimal],
  totalAccountOverdue: Option[BigDecimal],
  totalOverdue: Option[BigDecimal],
  totalNotYetDue: Option[BigDecimal],
  totalBalance: Option[BigDecimal],
  totalCredit: Option[BigDecimal],
  totalCleared: Option[BigDecimal]
)

object Totalisation {
  implicit val format: OFormat[Totalisation] = Json.format[Totalisation]
}

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

  def getPaymentType: PaymentType =
    documentType match {
      case None        => Unknown
      case Some(value) =>
        value match {
          case NewCharge | AmendedCharge => StandardPayment
          case InterestCharge            => Interest
          case Payment                   => Overpayment
          case _                         => Unknown
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
  def extractValue[A](value: Option[A]): A = value.getOrElse(throw new IllegalStateException())

  implicit val format: OFormat[DocumentDetails] = Json.format[DocumentDetails]
}

sealed trait FinancialDataDocumentType

object FinancialDataDocumentType {
  implicit val format: Format[FinancialDataDocumentType] = new Format[FinancialDataDocumentType] {
    override def reads(json: JsValue): JsResult[FinancialDataDocumentType] = json.validate[String] match {
      case JsSuccess(value, _) =>
        value match {
          case "TRM New Charge"    => JsSuccess(NewCharge)
          case "TRM Amend Charge"  => JsSuccess(AmendedCharge)
          case "Interest Document" => JsSuccess(InterestCharge)
          case "Payment"           => JsSuccess(Payment)
          case value               => JsSuccess(Other(value))
        }
      case e: JsError          => e
    }

    override def writes(o: FinancialDataDocumentType): JsValue = o match {
      case NewCharge      => JsString("TRM New Charge")
      case AmendedCharge  => JsString("TRM Amend Charge")
      case InterestCharge => JsString("Interest Document")
      case Payment        => JsString("Payment")
      case Other(value)   => JsString(value)
    }
  }
}

case object NewCharge extends FinancialDataDocumentType

case object AmendedCharge extends FinancialDataDocumentType

case object InterestCharge extends FinancialDataDocumentType

case object Payment extends FinancialDataDocumentType

case class Other(value: String) extends FinancialDataDocumentType

case class LineItemDetails(
  amount: Option[BigDecimal],
  chargeDescription: Option[String],
  clearingDate: Option[LocalDate],
  clearingDocument: Option[String],
  clearingReason: Option[String],
  netDueDate: Option[LocalDate],
  periodFromDate: Option[LocalDate],
  periodToDate: Option[LocalDate],
  periodKey: Option[String]
) {
  val isCleared: Boolean = clearingDate.nonEmpty
}

object LineItemDetails {
  implicit val format: OFormat[LineItemDetails] = Json.format[LineItemDetails]
}

case class PenaltyTotals(
  penaltyType: Option[String],
  penaltyStatus: Option[String],
  penaltyAmount: Option[BigDecimal],
  postedChargeReference: Option[String]
)

object PenaltyTotals {
  implicit val format: OFormat[PenaltyTotals] = Json.format[PenaltyTotals]
}
