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

package uk.gov.hmrc.economiccrimelevyaccount.utils

import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase

import java.util.UUID

class CorrelationIdHelperSpec extends SpecBase {

  "getOrCreateCorrelationID" should {
    "add the HEADER_X_CORRELATION_ID header to the HeaderCarrier extra headers if not present" in {
      val fakeRequest = FakeRequest.apply()

      val result = CorrelationIdHelper.getOrCreateCorrelationId(fakeRequest)

      result.extraHeaders.isEmpty         shouldBe false
      result.extraHeaders.head._2.isEmpty shouldBe false
      noException                           should be thrownBy UUID.fromString(result.extraHeaders.head._2)
    }

    "not add the HEADER_X_CORRELATION_ID header to the HeaderCarrier extra headers if present" in {
      val fakeRequest = FakeRequest.apply().withHeaders((HttpHeader.CorrelationId, "existingHeader"))

      val result = CorrelationIdHelper.getOrCreateCorrelationId(fakeRequest)

      result.extraHeaders                                           shouldBe empty
      result.headers(scala.Seq(HttpHeader.CorrelationId)).head._2 shouldBe "existingHeader"
    }
  }
}
