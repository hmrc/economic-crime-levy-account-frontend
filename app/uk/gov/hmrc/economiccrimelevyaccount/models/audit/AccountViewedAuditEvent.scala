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

package uk.gov.hmrc.economiccrimelevyaccount.models.audit

import play.api.libs.json.{JsValue, Json, OFormat}
import uk.gov.hmrc.economiccrimelevyaccount.models.{DocumentDetails, FinancialData, LineItemDetails, ObligationDetails, PenaltyTotals}
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.PaymentType

import java.time.LocalDate

case class AccountViewedAuditEvent(
  internalId: String,
  eclReference: String,
  obligationDetails: Seq[ObligationDetails],
  financialDetails: Option[AccountViewedAuditFinancialDetails]
) extends AuditEvent {
  override val auditType: String   = "AccountViewed"
  override val detailJson: JsValue = Json.toJson(this)
}

object AccountViewedAuditEvent {
  implicit val format: OFormat[AccountViewedAuditEvent] = Json.format[AccountViewedAuditEvent]
}

case class AccountViewedAuditFinancialDetails(
  totalAccountBalance: Option[BigDecimal],
  totalAccountOverdue: Option[BigDecimal],
  totalOverdue: Option[BigDecimal],
  totalNotYetDue: Option[BigDecimal],
  totalBalance: Option[BigDecimal],
  totalCredit: Option[BigDecimal],
  totalCleared: Option[BigDecimal],
  documentDetails: Option[Seq[AccountViewedAuditDocumentDetails]]
)

object AccountViewedAuditFinancialDetails {
  implicit val format: OFormat[AccountViewedAuditFinancialDetails] = Json.format[AccountViewedAuditFinancialDetails]

  def apply(response: FinancialData): AccountViewedAuditFinancialDetails =
    AccountViewedAuditFinancialDetails(
      response.totalisation.flatMap(_.totalAccountBalance),
      response.totalisation.flatMap(_.totalAccountOverdue),
      response.totalisation.flatMap(_.totalOverdue),
      response.totalisation.flatMap(_.totalNotYetDue),
      response.totalisation.flatMap(_.totalBalance),
      response.totalisation.flatMap(_.totalCredit),
      response.totalisation.flatMap(_.totalCleared),
      response.documentDetails.map(details => details.map(AccountViewedAuditDocumentDetails.apply))
    )
}

case class AccountViewedAuditDocumentDetails(
  chargeReferenceNumber: Option[String],
  issueDate: Option[String],
  interestPostedAmount: Option[BigDecimal],
  postingDate: Option[String],
  paymentType: PaymentType,
  penaltyTotals: Option[Seq[AccountViewedAuditPenaltyTotals]],
  lineItems: Option[Seq[AccountViewedAuditLineItem]]
)

object AccountViewedAuditDocumentDetails {
  implicit val format: OFormat[AccountViewedAuditDocumentDetails] = Json.format[AccountViewedAuditDocumentDetails]

  def apply(detail: DocumentDetails): AccountViewedAuditDocumentDetails =
    AccountViewedAuditDocumentDetails(
      detail.chargeReferenceNumber,
      detail.issueDate,
      detail.interestPostedAmount,
      detail.postingDate,
      detail.paymentType,
      detail.penaltyTotals.map(penaltyTotalsList => penaltyTotalsList.map(AccountViewedAuditPenaltyTotals.apply)),
      detail.lineItemDetails.map(lineItemDetailsList =>
        lineItemDetailsList.map(lineItem => AccountViewedAuditLineItem.apply(lineItem))
      )
    )
}

case class AccountViewedAuditPenaltyTotals(
  penaltyType: Option[String],
  penaltyStatus: Option[String],
  penaltyAmount: Option[BigDecimal]
)

object AccountViewedAuditPenaltyTotals {
  implicit val format: OFormat[AccountViewedAuditPenaltyTotals] = Json.format[AccountViewedAuditPenaltyTotals]

  def apply(penaltyTotal: PenaltyTotals): AccountViewedAuditPenaltyTotals =
    AccountViewedAuditPenaltyTotals(
      penaltyType = penaltyTotal.penaltyType,
      penaltyStatus = penaltyTotal.penaltyStatus,
      penaltyAmount = penaltyTotal.penaltyAmount
    )
}

case class AccountViewedAuditLineItem(
  chargeDescription: Option[String],
  clearingReason: Option[String],
  clearingDocument: Option[String],
  periodFromDate: Option[LocalDate],
  periodToDate: Option[LocalDate],
  periodKey: Option[String]
)

object AccountViewedAuditLineItem {
  implicit val format: OFormat[AccountViewedAuditLineItem] = Json.format[AccountViewedAuditLineItem]

  def apply(lineItem: LineItemDetails): AccountViewedAuditLineItem =
    AccountViewedAuditLineItem(
      lineItem.chargeDescription,
      lineItem.clearingReason,
      lineItem.clearingDocument,
      lineItem.periodFromDate,
      lineItem.periodToDate,
      lineItem.periodKey
    )
}
