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
  obligationDetails: Seq[ObligationDetails]
) extends AuditEvent {
  override val auditType: String   = "AccountViewed"
  override val detailJson: JsValue = Json.toJson(this)
}

object AccountViewedAuditEvent {
  implicit val format: OFormat[AccountViewedAuditEvent] = Json.format[AccountViewedAuditEvent]
}
