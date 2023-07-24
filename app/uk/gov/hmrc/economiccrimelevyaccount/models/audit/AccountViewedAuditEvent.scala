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
import uk.gov.hmrc.economiccrimelevyaccount.models.ObligationDetails

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
  documentDetails: Option[Seq[AccountViewedAuditDocumentDetails]]
)

object AccountViewedAuditFinancialDetails {
  implicit val format: OFormat[AccountViewedAuditFinancialDetails] = Json.format[AccountViewedAuditFinancialDetails]
}

case class AccountViewedAuditDocumentDetails(
  chargeReferenceNumber: Option[String],
  issueDate: Option[String],
  interestPostedAmount: Option[BigDecimal],
  paidAmount: Option[BigDecimal],
  postingDate: Option[String],
  penaltyTotals: Option[Seq[AccountViewedAuditPenaltyTotals]],
  lineItems: Option[Seq[AccountViewedAuditLineItem]]
)

object AccountViewedAuditDocumentDetails {
  implicit val format: OFormat[AccountViewedAuditDocumentDetails] = Json.format[AccountViewedAuditDocumentDetails]
}

case class AccountViewedAuditPenaltyTotals(
  penaltyType: Option[String],
  penaltyStatus: Option[String],
  penaltyAmount: Option[BigDecimal]
)

object AccountViewedAuditPenaltyTotals {
  implicit val format: OFormat[AccountViewedAuditPenaltyTotals] = Json.format[AccountViewedAuditPenaltyTotals]
}

case class AccountViewedAuditLineItem(
  chargeDescription: Option[String],
  periodFromDate: Option[String],
  periodToDate: Option[String],
  periodKey: Option[String]
)

object AccountViewedAuditLineItem {
  implicit val format: OFormat[AccountViewedAuditLineItem] = Json.format[AccountViewedAuditLineItem]
}
