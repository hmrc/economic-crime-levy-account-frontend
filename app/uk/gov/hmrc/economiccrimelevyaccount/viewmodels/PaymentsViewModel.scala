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

package uk.gov.hmrc.economiccrimelevyaccount.viewmodels

import play.api.libs.json._
import uk.gov.hmrc.economiccrimelevyaccount.models.EclSubscriptionStatus.Subscribed
import uk.gov.hmrc.economiccrimelevyaccount.models.{EclReference, EclSubscriptionStatus}

import java.time.LocalDate

case class PaymentsViewModel(
  outstandingPayments: Seq[OutstandingPayments],
  paymentHistory: Seq[PaymentHistory],
  eclRegistrationReference: EclReference,
  eclSubscriptionStatus: EclSubscriptionStatus
) {
  val isSubscribed: Boolean =
    eclSubscriptionStatus.subscriptionStatus == Subscribed(eclRegistrationReference.value)
}

case class OutstandingPayments(
  paymentDueDate: LocalDate,
  chargeReference: String,
  fyFrom: LocalDate,
  fyTo: LocalDate,
  amount: BigDecimal,
  paymentStatus: PaymentStatus,
  paymentType: PaymentType,
  interestChargeReference: Option[String]
)

case class PaymentHistory(
  paymentDate: LocalDate,
  chargeReference: Option[String],
  fyFrom: Option[LocalDate],
  fyTo: Option[LocalDate],
  amount: BigDecimal,
  paymentStatus: PaymentStatus,
  paymentType: PaymentType,
  paymentDocument: String,
  refundAmount: BigDecimal
)
sealed trait PaymentStatus

object PaymentStatus {
  case object Overdue extends PaymentStatus

  case object Due extends PaymentStatus

  case object Paid extends PaymentStatus

  case object PartiallyPaid extends PaymentStatus
}

sealed trait PaymentType

object PaymentType {
  case object StandardPayment extends PaymentType
  case object Overpayment extends PaymentType
  case object Interest extends PaymentType
  case object Unknown extends PaymentType

  implicit val format: Format[PaymentType] = new Format[PaymentType] {
    override def reads(json: JsValue): JsResult[PaymentType] = json.validate[String] match {
      case JsSuccess(value, _) =>
        value match {
          case "StandardPayment" => JsSuccess(StandardPayment)
          case "Interest"        => JsSuccess(Interest)
          case "Overpayment"     => JsSuccess(Overpayment)
          case _                 => JsSuccess(Unknown)
        }
      case e: JsError          => e
    }

    override def writes(o: PaymentType): JsValue = o match {
      case StandardPayment => JsString("StandardPayment")
      case Interest        => JsString("Interest")
      case Overpayment     => JsString("Overpayment")
      case Unknown         => JsString("Unknown")
    }
  }

}
