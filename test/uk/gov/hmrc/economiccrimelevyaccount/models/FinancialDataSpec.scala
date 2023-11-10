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

class FinancialDataSpec extends SpecBase {

  "FinancialDataResponse refundAmount" should {

    val twoHundred = BigDecimal(200)

    def setup(
      dataResponse: ValidFinancialDataResponse,
      overpaymentAmount: BigDecimal,
      contractObjectNumberParam: String,
      regime: String,
      secondRecordDocumentType: FinancialDataDocumentType
    ): FinancialData = {
      val documentDetails = dataResponse.financialDataResponse.documentDetails.get.head
      dataResponse.financialDataResponse.copy(
        documentDetails = Some(
          Seq(
            documentDetails.copy(
              documentTotalAmount = Some(overpaymentAmount),
              contractObjectNumber = Some(contractObjectNumberParam),
              contractObjectType = Some(regime),
              documentType = Some(NewCharge)
            ),
            documentDetails.copy(
              documentTotalAmount = Some(overpaymentAmount),
              contractObjectNumber = Some(contractObjectNumberParam),
              contractObjectType = Some(regime),
              documentType = Some(secondRecordDocumentType)
            )
          )
        )
      )
    }

    def setupDocumentType(
      dataResponse: ValidFinancialDataResponse,
      documentType: FinancialDataDocumentType
    ): DocumentDetails =
      dataResponse.financialDataResponse.documentDetails.get.head
        .copy(documentType = Some(documentType))

    "return None documents that do not have an overpayment" in forAll {
      (
        dataResponse: ValidFinancialDataResponse
      ) =>
        val details = setup(dataResponse, twoHundred, "1234567890", "ECL", NewCharge)

        details.refundAmount("1234567890") should equal(Some(BigDecimal(0)))
    }

    "return Some of correct amount when we have an overpayment" in forAll {
      (
        dataResponse: ValidFinancialDataResponse
      ) =>
        val details = setup(dataResponse, twoHundred, "1234567890", "ECL", Payment)

        details.refundAmount("1234567890") should equal(Some(BigDecimal(200)))
    }

    "return Payment obligation for NewCharge documentType" in forAll {
      (
        dataResponse: ValidFinancialDataResponse
      ) =>
        val details = setupDocumentType(dataResponse, NewCharge)
        details.paymentType should equal(StandardPayment)
    }

    "return Interest obligation for InterestCharge documentType" in forAll {
      (
        dataResponse: ValidFinancialDataResponse
      ) =>
        val details = setupDocumentType(dataResponse, InterestCharge)
        details.paymentType should equal(Interest)
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

  }
}
