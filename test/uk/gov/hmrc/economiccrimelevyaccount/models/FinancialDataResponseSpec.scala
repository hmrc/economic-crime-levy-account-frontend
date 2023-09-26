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

import uk.gov.hmrc.economiccrimelevyaccount.ValidFinancialDataResponse
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.PaymentType._

class FinancialDataResponseSpec extends SpecBase {

  "FinancialDataResponse refundAmount" should {

    val zero       = BigDecimal(0)
    val oneHundred = BigDecimal(100)
    val twoHundred = BigDecimal(200)

    def setup(dataResponse: ValidFinancialDataResponse, total: BigDecimal, paid: BigDecimal): DocumentDetails =
      dataResponse.financialDataResponse.documentDetails.get.head
        .copy(
          documentClearedAmount = Some(paid),
          documentTotalAmount = Some(total)
        )

    def setupDocumentType(
      dataResponse: ValidFinancialDataResponse,
      documentType: FinancialDataDocumentType
    ): DocumentDetails =
      dataResponse.financialDataResponse.documentDetails.get.head
        .copy(documentType = Some(documentType))

    "return zero for partially paid document" in forAll {
      (
        dataResponse: ValidFinancialDataResponse
      ) =>
        val details = setup(dataResponse, twoHundred, oneHundred)

        details.refundAmount should equal(zero)
    }

    "return zero for fully paid document" in forAll {
      (
        dataResponse: ValidFinancialDataResponse
      ) =>
        val details = setup(dataResponse, oneHundred, oneHundred)

        details.refundAmount should equal(zero)
    }

    "return correct amount for overpaid document" in forAll {
      (
        dataResponse: ValidFinancialDataResponse
      ) =>
        val details = setup(dataResponse, oneHundred, twoHundred)

        details.refundAmount should equal(oneHundred)
    }

    "return Payment obligation for NewCharge documentType" in forAll {
      (
        dataResponse: ValidFinancialDataResponse
      ) =>
        val details = setupDocumentType(dataResponse, NewCharge)
        details.getPaymentType should equal(Payment)
    }

    "return Interest obligation for InterestCharge documentType" in forAll {
      (
        dataResponse: ValidFinancialDataResponse
      ) =>
        val details = setupDocumentType(dataResponse, InterestCharge)
        details.getPaymentType should equal(Interest)
    }

    "return interest charge reference when documentType is InterestCharge" in forAll {
      (
        dataResponse: ValidFinancialDataResponse
      ) =>
        val details = setupDocumentType(dataResponse, InterestCharge)
        details.getInterestChargeReference should equal(Some("test-ecl-registration-reference"))
    }

    "return None for interest charge reference when documentType is NewCharge" in forAll {
      (
        dataResponse: ValidFinancialDataResponse
      ) =>
        val details = setupDocumentType(dataResponse, NewCharge)
        details.getInterestChargeReference should equal(None)
    }

    "throw exception if None is passed in extractValue method" in {
      an[IllegalStateException] should be thrownBy DocumentDetails.extractValue(None)
    }
  }
}
