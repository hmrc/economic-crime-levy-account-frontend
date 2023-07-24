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

package uk.gov.hmrc.economiccrimelevyaccount.services

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.http.Status.CREATED
import uk.gov.hmrc.economiccrimelevyaccount.base.{OpsTestData, SpecBase}
import uk.gov.hmrc.economiccrimelevyaccount.connectors.{OpsApiError, OpsConnector}
import uk.gov.hmrc.economiccrimelevyaccount.controllers.routes
import uk.gov.hmrc.economiccrimelevyaccount.models.{OpsJourneyRequest, OpsJourneyResponse}

import java.time.LocalDate
import scala.concurrent.Future

class OpsServiceSpec extends SpecBase with OpsTestData {
  val mockOpsConnector: OpsConnector = mock[OpsConnector]
  val service                        = new OpsService(mockOpsConnector, appConfig)
  val expectedUrl: String            = "http://www.bbc.co.uk"
  val opsApiError                    = OpsApiError(
    CREATED,
    "Invalid Json"
  )

  "startOpsJourney" should {
    "return journey info if successful" in forAll { (chargeReference: String, amount: BigDecimal) =>
      val opsJourneyResponse = OpsJourneyResponse(
        "",
        expectedUrl
      )

      val url = appConfig.host + routes.AccountController.onPageLoad().url

      val opsJourneyRequest = OpsJourneyRequest(
        chargeReference,
        amount * 100,
        url,
        url,
        None
      )

      when(
        mockOpsConnector.createOpsJourney(
          ArgumentMatchers.eq(opsJourneyRequest)
        )(any())
      )
        .thenReturn(Future.successful(Right(opsJourneyResponse)))

      val result = await(service.startOpsJourney(chargeReference, amount, None))

      result shouldBe Right(opsJourneyResponse)
    }
  }

  "return error if error when creating journey" in forAll { (chargeReference: String, amount: BigDecimal) =>
    val url = appConfig.host + routes.AccountController.onPageLoad().url

    val opsJourneyRequest = OpsJourneyRequest(
      chargeReference,
      amount * 100,
      url,
      url,
      None
    )

    when(
      mockOpsConnector.createOpsJourney(
        ArgumentMatchers.eq(opsJourneyRequest)
      )(any())
    ).thenReturn(Future.successful(Left(opsApiError)))

    val result = await(service.startOpsJourney(chargeReference, amount, None))

    result shouldBe Left(opsApiError)
  }

  "getTotalPaid" should {
    "return sum of successful payments if successful" in forAll { (chargeReference: String, date: LocalDate) =>
      when(
        mockOpsConnector.getPayments(
          ArgumentMatchers.eq(chargeReference)
        )(any())
      )
        .thenReturn(Future.successful(Right(payments(date))))

      val result = await(service.getTotalPaid(chargeReference))

      result shouldBe 150
    }
  }

  "return zero if error when getting payments" in forAll { (chargeReference: String) =>
    when(
      mockOpsConnector.getPayments(
        ArgumentMatchers.eq(chargeReference)
      )(any())
    ).thenReturn(Future.successful(Left(opsApiError)))

    val result = await(service.getTotalPaid(chargeReference))

    result shouldBe 0
  }
}
