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

package uk.gov.hmrc.economiccrimelevyaccount.controllers

import play.api.http.Status.BAD_GATEWAY
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.models.errors.{EclAccountError, EclRegistrationError, OpsError, ResponseError}

class ErrorHandlerSpec extends SpecBase with ErrorHandler {

  "eclAccountErrorConverter" should {
    "return ResponseError.internalServiceError when EclAccountError.InternalUnexpectedError.InternalUnexpectedError is converted" in {
      val message   = "Error message"
      val exception = new Exception()

      val eclAccountError = EclAccountError.InternalUnexpectedError(message, Some(exception))

      val result = eclAccountErrorConverter.convert(eclAccountError)

      result shouldBe ResponseError.internalServiceError(message = message, cause = Some(exception))
    }
  }

  "opsErrorConverter" should {
    "return ResponseError.badGateway when OpsError.BadGateway is converted" in {

      val code    = BAD_GATEWAY
      val message = "Error message"

      val opsError = OpsError.BadGateway(message, code)

      val result = opsErrorConverter.convert(opsError)

      result shouldBe ResponseError.badGateway(message, code)
    }

    "return ResponseError.internalServiceError when OpsError.InternalUnexpectedError.InternalUnexpectedError is converted" in {

      val message   = "Error message"
      val exception = new Exception()

      val opsError = OpsError.InternalUnexpectedError(message, Some(exception))

      val result = opsErrorConverter.convert(opsError)

      result shouldBe ResponseError.internalServiceError(message = message, cause = Some(exception))
    }
  }

  "eclRegistrationErrorConverter" should {
    "return ResponseError.badGateway when EclRegistrationError.BadGateway is converted" in {

      val code    = BAD_GATEWAY
      val message = "Error message"

      val eclRegistrationError = EclRegistrationError.BadGateway(message, code)

      val result = eclRegistrationErrorConverter.convert(eclRegistrationError)

      result shouldBe ResponseError.badGateway(message, code)
    }

    "return ResponseError.internalServiceError when EclRegistrationError.InternalUnexpectedError.InternalUnexpectedError is converted" in {

      val message   = "Error message"
      val exception = new Exception()

      val eclRegistrationError = EclRegistrationError.InternalUnexpectedError(message, Some(exception))

      val result = eclRegistrationErrorConverter.convert(eclRegistrationError)

      result shouldBe ResponseError.internalServiceError(message = message, cause = Some(exception))
    }
  }
}
