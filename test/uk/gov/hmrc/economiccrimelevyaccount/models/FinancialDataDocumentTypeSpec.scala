/*
 * Copyright 2024 HM Revenue & Customs
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

import play.api.libs.json.{JsResultException, JsString, Json}
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyaccount.utils.DocumentType

class FinancialDataDocumentTypeSpec extends SpecBase {

  "reads" should {
    "return the financial data document type deserialized from it's JSON representation" in forAll {
      (financialDataDocumentType: FinancialDataDocumentType) =>
        val json = Json.toJson(financialDataDocumentType)

        json.as[FinancialDataDocumentType] shouldBe financialDataDocumentType
    }

    "return error when trying to deserialize unknown financial data document type" in forAll {
      (financialDataDocumentType: FinancialDataDocumentType) =>
        val json = Json.toJson(100)

        val error = intercept[JsResultException] {
          json.as[FinancialDataDocumentType] shouldBe financialDataDocumentType
        }

        assert(error.errors.nonEmpty)
    }
  }

  "writes" should {
    "return the serialized String from it's financial data document type representation" in forAll {
      (financialDataDocumentType: FinancialDataDocumentType) =>
        val json = Json.toJson(financialDataDocumentType)

        val matchedType = financialDataDocumentType match {
          case NewCharge      => JsString(DocumentType.trmNewCharge)
          case AmendedCharge  => JsString(DocumentType.trmAmendCharge)
          case InterestCharge => JsString(DocumentType.interestDocument)
          case Payment        => JsString(DocumentType.payment)
        }

        json shouldBe matchedType

    }
  }

}
