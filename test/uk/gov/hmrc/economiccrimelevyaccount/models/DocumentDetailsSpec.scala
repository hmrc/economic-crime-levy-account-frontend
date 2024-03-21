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

import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.generators.CachedArbitraries._
class DocumentDetailsSpec extends SpecBase {

  "filterInPayments" should {
    "return DocumentDetails if documentType is NewCharge" in forAll { (documentDetails: DocumentDetails) =>
      val validDocumentDetails = documentDetails.copy(documentType = Some(NewCharge))
      val seqOfDocumentDetails = Seq(validDocumentDetails)

      seqOfDocumentDetails.collect(DocumentDetails.filterInPayments).size shouldBe 1
    }
  }

  "isNewestLineItem" should {
    "return true when passed in lineItem is new line item" in forAll {
      (documentDetails: DocumentDetails, items: LineItemDetails) =>
        val validDocumentDetails = documentDetails.copy(lineItemDetails = Some(Seq(items)))

        validDocumentDetails.isNewestLineItem(items) shouldBe true
    }

    "return false when there is no previous line item details" in forAll {
      (documentDetails: DocumentDetails, items: LineItemDetails) =>
        val validDocumentDetails = documentDetails.copy(lineItemDetails = None)

        validDocumentDetails.isNewestLineItem(items) shouldBe false
    }
  }

  "getInterestChargeReference" should {
    "return None when documentType is None as well" in forAll { (documentDetails: DocumentDetails) =>
      val validDocumentDetails = documentDetails.copy(documentType = None)

      validDocumentDetails.getInterestChargeReference shouldBe None
    }
  }
}
