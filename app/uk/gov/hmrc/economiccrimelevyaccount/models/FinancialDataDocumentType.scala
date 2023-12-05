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

import play.api.libs.json.{Format, JsError, JsResult, JsString, JsSuccess, JsValue}
import uk.gov.hmrc.economiccrimelevyaccount.utils.DocumentTypeValues

sealed trait FinancialDataDocumentType

case object NewCharge extends FinancialDataDocumentType

case object AmendedCharge extends FinancialDataDocumentType

case object InterestCharge extends FinancialDataDocumentType

case object Payment extends FinancialDataDocumentType

object FinancialDataDocumentType {
  implicit val format: Format[FinancialDataDocumentType] = new Format[FinancialDataDocumentType] {
    override def reads(json: JsValue): JsResult[FinancialDataDocumentType] = json.validate[String] match {
      case JsSuccess(value, _) =>
        value match {
          case DocumentTypeValues.TrmNewCharge     => JsSuccess(NewCharge)
          case DocumentTypeValues.TrmAmendCharge   => JsSuccess(AmendedCharge)
          case DocumentTypeValues.InterestDocument => JsSuccess(InterestCharge)
          case DocumentTypeValues.Payment          => JsSuccess(Payment)
        }
      case e: JsError          => e
    }

    override def writes(o: FinancialDataDocumentType): JsValue = o match {
      case NewCharge      => JsString(DocumentTypeValues.TrmNewCharge)
      case AmendedCharge  => JsString(DocumentTypeValues.TrmAmendCharge)
      case InterestCharge => JsString(DocumentTypeValues.InterestDocument)
      case Payment        => JsString(DocumentTypeValues.Payment)
    }
  }
}
