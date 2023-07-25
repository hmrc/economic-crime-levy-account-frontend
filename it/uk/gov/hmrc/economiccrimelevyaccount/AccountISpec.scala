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

class AccountISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.AccountController.onPageLoad().url}" should {
    behave like authorisedActionRoute(routes.AccountController.onPageLoad())

    "respond with 200 status and the start HTML view" in {
      stubAuthorised()

      val eclRegistrationReference = random[String]
      val eclRegistrationDate      = "20230901"

      val obligationData = random[ObligationData]
      val chargeReference = random[String]

      stubQueryKnownFacts(eclRegistrationReference, eclRegistrationDate)
      stubGetObligations(obligationData)
      stubFinancialData(chargeReference)
      stubGetPayments(chargeReference)

      val result = callRoute(FakeRequest(routes.AccountController.onPageLoad()))

      status(result) shouldBe OK
      html(result)     should include("Your Economic Crime Levy account")
    }
  }

}
