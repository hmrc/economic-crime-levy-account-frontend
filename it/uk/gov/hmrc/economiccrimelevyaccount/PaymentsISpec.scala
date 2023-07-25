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

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyaccount.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyaccount.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyaccount.controllers.routes
import uk.gov.hmrc.economiccrimelevyaccount.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyaccount.models.ObligationData

import java.time.LocalDate

class PaymentsISpec extends ISpecBase with AuthorisedBehaviour with OpsTestData {
  val expectedUrl = "http://bc.co.uk"

  s"GET ${routes.PaymentsController.onPageLoad().url}" should {
    behave like authorisedActionRoute(routes.AccountController.onPageLoad())

    "respond with 303 status and the expected HTML view" in {
      stubAuthorised()

      val obligationData = random[ObligationData]
      val chargeReference = random[String]

      stubGetObligations(obligationData)
      stubFinancialData(chargeReference)
      stubGetPayments(chargeReference)
      stubStartJourney(expectedUrl)

      val result = callRoute(FakeRequest(routes.PaymentsController.onPageLoad()))

      status(result) shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe expectedUrl
    }
  }

}
