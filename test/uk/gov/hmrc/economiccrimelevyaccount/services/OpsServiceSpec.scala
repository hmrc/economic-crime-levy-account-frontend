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
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.connectors.{OpsConnector, OpsJourneyError}
import uk.gov.hmrc.economiccrimelevyaccount.controllers.routes
import uk.gov.hmrc.economiccrimelevyaccount.models.{OpsJourneyRequest, OpsJourneyResponse}

import scala.concurrent.Future

class OpsServiceSpec extends SpecBase {
  val mockOpsConnector: OpsConnector = mock[OpsConnector]
  val service                        = new OpsService(mockOpsConnector, appConfig)
  val expectedUrl: String            = "http://www.bbc.co.uk"

  "startOpsJourney" should {
    "redirect to returned URL if successful" in forAll { (chargeReference: String, amount: BigDecimal) =>
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
        .thenReturn(Future.successful(Left(opsJourneyResponse)))

      val result = await(service.startOpsJourney(chargeReference, amount, None))

      result shouldBe Redirect(expectedUrl)
    }
  }

  "redirect to account page if error" in forAll { (chargeReference: String, amount: BigDecimal) =>
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
      .thenReturn(
        Future.successful(
          Right(
            OpsJourneyError(
              201,
              "Invalid Json"
            )
          )
        )
      )

    val result = await(service.startOpsJourney(chargeReference, amount, None))

    result shouldBe Redirect(routes.AccountController.onPageLoad())
  }
}
