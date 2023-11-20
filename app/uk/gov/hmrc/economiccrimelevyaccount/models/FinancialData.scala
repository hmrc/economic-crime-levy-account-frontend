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

import play.api.libs.json._
import uk.gov.hmrc.economiccrimelevyaccount.utils.Constants
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.PaymentType._

import java.time.LocalDate

case class FinancialData(totalisation: Option[Totalisation], documentDetails: Option[Seq[DocumentDetails]]) {

  private def getDocumentsByContractObject(
    contractObjectNumber: String
  ): Option[Seq[DocumentDetails]] =
    documentDetails.map {
      _.filter(document =>
        document.contractObjectType.contains(Constants.CONTRACT_OBJECT_TYPE_ECL)
          && document.contractObjectNumber.contains(contractObjectNumber)
      )
    }

  def refundAmount(contractObjectNumber: String): Option[BigDecimal] =
    getDocumentsByContractObject(contractObjectNumber).map {
      _.collect(outOverPaymentPredicate)
        .flatMap(_.documentTotalAmount)
        .sum
    }

  private def outOverPaymentPredicate: PartialFunction[DocumentDetails, DocumentDetails] = {
    case document: DocumentDetails if document.paymentType == Overpayment => document
  }

  val latestFinancialObligation: Option[DocumentDetails] =
    documentDetails
      .map(_.collect {
        case docDetails
            if (docDetails.isType(NewCharge)
              || docDetails.isType(AmendedCharge)
              || docDetails.isType(InterestCharge))
              && !docDetails.isCleared =>
          docDetails
      })
      .flatMap(_.sortBy(_.postingDate).headOption)

  def getFinancialObligation(chargeReference: String): Option[DocumentDetails] =
    documentDetails
      .map(_.collect {
        case docDetails if docDetails.chargeReferenceNumber.contains(chargeReference) => docDetails
      })
      .flatMap(_.headOption)
}

object FinancialData {

  implicit val format: OFormat[FinancialData] = Json.format[FinancialData]
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

case class PenaltyTotals(
  penaltyType: Option[String],
  penaltyStatus: Option[String],
  penaltyAmount: Option[BigDecimal],
  postedChargeReference: Option[String]
)

object PenaltyTotals {
  implicit val format: OFormat[PenaltyTotals] = Json.format[PenaltyTotals]
}
