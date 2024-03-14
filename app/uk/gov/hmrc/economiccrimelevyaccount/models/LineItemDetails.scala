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

import java.time.LocalDate

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

  def useOnlyRegularLineItemDetails: PartialFunction[LineItemDetails, LineItemDetails] = {
    case lineItemDetails: LineItemDetails
        if containsString(lineItemDetails.clearingReason, "automatic clearing")
          | containsString(lineItemDetails.clearingReason, "incoming payment")
          | containsString(lineItemDetails.clearingReason, "reversal") =>
      lineItemDetails
  }

  def isCleared: PartialFunction[LineItemDetails, LineItemDetails] = {
    case lineItemDetails: LineItemDetails if lineItemDetails.isCleared =>
      lineItemDetails
  }

  private def containsString(value: Option[String], expectedMatch: String) =
    value.exists(str => expectedMatch.equalsIgnoreCase(str))
}
