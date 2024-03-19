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

package uk.gov.hmrc.economiccrimelevyaccount.models.errors

import play.api.libs.json.{JsResultException, JsString, Json}
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.generators.CachedArbitraries.arbErrorCodes

class ErrorCodeSpec extends SpecBase {
  "errorCodeWrites" should {
    "return the String from it's Error code representation" in forAll { (errorCode: ErrorCode) =>
      val json = Json.toJson(errorCode)
      json shouldBe JsString(errorCode.code)
    }
  }

  "errorCodeReads" should {
    "return the error type deserialized from it's JSON representation" in forAll { (errorCode: ErrorCode) =>
      val json = Json.toJson(errorCode.code)
      json.as[ErrorCode] shouldBe errorCode
    }

    "return error when trying to deserialize unknown error code" in forAll { (errorCode: ErrorCode) =>
      val json  = Json.toJson("Unknown")
      val error = intercept[JsResultException] {
        json.as[ErrorCode] shouldBe errorCode
      }
      assert(error.errors.nonEmpty)
    }
  }
}
