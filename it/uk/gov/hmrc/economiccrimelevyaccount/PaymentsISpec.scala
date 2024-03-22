/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.economiccrimelevyaccount

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.Eventually.eventually
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyaccount.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyaccount.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyaccount.controllers.routes
import uk.gov.hmrc.economiccrimelevyaccount.utils.HttpHeader

class PaymentsISpec extends ISpecBase with AuthorisedBehaviour {

  val expectedUrl                       = "http://test-url.co.uk"
  private val expectedCallsOnRetry: Int = 4

  s"GET ${routes.PaymentsController.onPageLoad(None).url}" should {
    behave like authorisedActionRoute(routes.AccountController.onPageLoad())

    val chargeReference = "XMECL0000000006"

    "respond with 200 status and the start HTML view with no charge reference" in {
      stubAuthorised()

      stubFinancialData()
      stubStartJourney(expectedUrl)

      val result = callRoute(FakeRequest(routes.PaymentsController.onPageLoad(None)))

      status(result)                 shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe expectedUrl

      verify(
        1,
        getRequestedFor(urlEqualTo(s"/economic-crime-levy-account/financial-data"))
          .withHeader(HttpHeader.CorrelationId, matching(uuidRegex))
      )

      verify(
        1,
        postRequestedFor(urlEqualTo(s"/pay-api/economic-crime-levy/journey/start"))
          .withHeader(HttpHeader.CorrelationId, matching(uuidRegex))
      )

    }

    "respond with 200 status and the start HTML view with charge reference" in {
      stubAuthorised()

      stubFinancialData()
      stubStartJourney(expectedUrl)

      val result = callRoute(FakeRequest(routes.PaymentsController.onPageLoad(Some(chargeReference))))

      status(result)                 shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe expectedUrl

      verify(
        1,
        getRequestedFor(urlEqualTo(s"/economic-crime-levy-account/financial-data"))
          .withHeader(HttpHeader.CorrelationId, matching(uuidRegex))
      )

      verify(
        1,
        postRequestedFor(urlEqualTo(s"/pay-api/economic-crime-levy/journey/start"))
          .withHeader(HttpHeader.CorrelationId, matching(uuidRegex))
      )

    }

    "retry the get submission call 3 times after the initial attempt if it fails with a 500 INTERNAL_SERVER_ERROR response" in {

      stubAuthorised()

      stubFinancialDataError()
      stubStartJourney(expectedUrl)

      val result = callRoute(FakeRequest(routes.PaymentsController.onPageLoad(Some(chargeReference))))

      status(result) shouldBe INTERNAL_SERVER_ERROR

      eventually {
        verify(
          expectedCallsOnRetry,
          getRequestedFor(urlEqualTo(s"/economic-crime-levy-account/financial-data"))
            .withHeader(HttpHeader.CorrelationId, matching(uuidRegex))
        )
      }
    }

  }
}
