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

import java.time.LocalDate

case class FinancialDetails(
  amount: BigDecimal,
  fromDate: Option[LocalDate],
  toDate: Option[LocalDate],
  periodKey: Option[String],
  chargeReference: Option[String],
  paymentType: PaymentType
) {
  private val dueMonth           = 9
  private val dueDay             = 30
  val dueDate: Option[LocalDate] = toDate.map(date => LocalDate.of(date.getYear, dueMonth, dueDay))

  def isPaymentType(value: PaymentType): Boolean = paymentType != value

  def isOverdue: Option[Boolean] = dueDate.map(date => LocalDate.now().isAfter(date))
}

object FinancialDetails {
  implicit val format: OFormat[FinancialDetails] = Json.format[FinancialDetails]

  def applyOptional(documentDetails: DocumentDetails): Option[FinancialDetails] =
    documentDetails.documentOutstandingAmount.map { outstandingAmount =>
      val optionalFirstItemInItemDetails = documentDetails.lineItemDetails.flatMap(_.headOption)

      FinancialDetails(
        outstandingAmount,
        optionalFirstItemInItemDetails.flatMap(_.periodFromDate),
        optionalFirstItemInItemDetails.flatMap(_.periodToDate),
        optionalFirstItemInItemDetails.flatMap(_.periodKey),
        documentDetails.chargeReferenceNumber,
        documentDetails.paymentType
      )
    }
}
