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

package uk.gov.hmrc.economiccrimelevyaccount.services

import play.api.http.Status.INTERNAL_SERVER_ERROR
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.connectors.EclRegistrationConnector
import uk.gov.hmrc.economiccrimelevyaccount.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyaccount.models.EclSubscriptionStatus
import uk.gov.hmrc.economiccrimelevyaccount.models.errors.EclRegistrationError
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future

class EclRegistrationServiceSpec extends SpecBase {

  private val mockEclRegistrationConnector: EclRegistrationConnector = mock[EclRegistrationConnector]
  private val service: EclRegistrationService                        = new EclRegistrationService(mockEclRegistrationConnector)

  "getSubscriptionStatus" should {
    "return an EclSubscriptionStatus when EclRegistrationConnector returns an EclSubscriptionStatus" in forAll {
      (eclRegistrationReference: String, eclSubscriptionStatus: EclSubscriptionStatus) =>
        when(mockEclRegistrationConnector.getSubscriptionStatus(eclRegistrationReference))
          .thenReturn(Future.successful(eclSubscriptionStatus))

        val result: Either[EclRegistrationError, EclSubscriptionStatus] =
          await(service.getSubscriptionStatus(eclRegistrationReference).value)

        result shouldBe Right(eclSubscriptionStatus)
    }

    "return EclRegistrationError.BadGateway when when call to EclRegistrationConnector fails with 5xx error" in forAll {
      (eclRegistrationReference: String, errorMessage: String) =>
        val errorCode = INTERNAL_SERVER_ERROR

        when(mockEclRegistrationConnector.getSubscriptionStatus(eclRegistrationReference))
          .thenReturn(Future.failed(UpstreamErrorResponse.apply(errorMessage, errorCode)))

        val result: Either[EclRegistrationError, EclSubscriptionStatus] =
          await(service.getSubscriptionStatus(eclRegistrationReference).value)

        result shouldBe Left(EclRegistrationError.BadGateway(errorMessage, errorCode))
    }

    "return EclRegistrationError.InternalUnexpectedError when when call to EclRegistrationConnector fails with an unexpected error" in forAll {
      (eclRegistrationReference: String, errorMessage: String) =>
        val throwable: Exception = new Exception(errorMessage)

        when(mockEclRegistrationConnector.getSubscriptionStatus(eclRegistrationReference))
          .thenReturn(Future.failed(throwable))

        val result: Either[EclRegistrationError, EclSubscriptionStatus] =
          await(service.getSubscriptionStatus(eclRegistrationReference).value)

        result shouldBe Left(EclRegistrationError.InternalUnexpectedError(Some(throwable), Some(errorMessage)))
    }
  }
}
