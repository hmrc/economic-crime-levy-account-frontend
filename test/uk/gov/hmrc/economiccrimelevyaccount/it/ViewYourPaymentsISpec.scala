/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.economiccrimelevyaccount.it

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.Eventually.eventually
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyaccount.controllers.routes
import uk.gov.hmrc.economiccrimelevyaccount.it.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyaccount.it.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyaccount.utils.HttpHeader

class ViewYourPaymentsISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.ViewYourPaymentsController.onPageLoad().url}" should {
    behave like authorisedActionRoute(routes.ViewYourPaymentsController.onPageLoad())

    "respond with 200 status and return correct view" in {
      stubAuthorised()
      stubFinancialData()
      stubGetSubscriptionStatus(testEclReference, testSubscribedSubscriptionStatus)

      val result = callRoute(FakeRequest(routes.ViewYourPaymentsController.onPageLoad()))

      status(result) shouldBe OK
      html(result)     should include("Your Economic Crime Levy payments")

      verify(
        1,
        getRequestedFor(urlEqualTo(s"/economic-crime-levy-account/financial-data"))
          .withHeader(HttpHeader.xCorrelationId, matching(uuidRegex))
      )

      verify(
        1,
        getRequestedFor(
          urlEqualTo(s"/economic-crime-levy-registration/subscription-status/ZECL/${testEclReference.value}")
        )
          .withHeader(HttpHeader.xCorrelationId, matching(uuidRegex))
      )
    }
    "retry the get submission call 3 times after the initial attempt if it fails with a 500 INTERNAL_SERVER_ERROR response" in {
      stubAuthorised()
      stubFinancialDataError()

      val result = callRoute(FakeRequest(routes.ViewYourPaymentsController.onPageLoad()))

      status(result) shouldBe INTERNAL_SERVER_ERROR

      eventually {
        verify(
          1,
          getRequestedFor(urlEqualTo(s"/economic-crime-levy-account/financial-data"))
            .withHeader(HttpHeader.xCorrelationId, matching(uuidRegex))
        )
      }
    }
  }

}
