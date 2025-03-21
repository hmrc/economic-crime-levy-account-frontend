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

package uk.gov.hmrc.economiccrimelevyaccount.it

import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyaccount.controllers.routes
import uk.gov.hmrc.economiccrimelevyaccount.it.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyaccount.it.behaviours.AuthorisedBehaviour

class NotableErrorISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.NotableErrorController.notRegistered().url}"            should {
    "redirect to enrolment service and return 303 status" in {
      stubAuthorised()

      val result = callRoute(FakeRequest(routes.NotableErrorController.notRegistered()))

      status(result) shouldBe SEE_OTHER
    }
  }

  s"GET ${routes.NotableErrorController.agentCannotAccessService().url}" should {
    "respond with 200 status and the agent cannot access service HTML view" in {
      stubAuthorised()

      val result = callRoute(FakeRequest(routes.NotableErrorController.agentCannotAccessService()))

      status(result) shouldBe OK
      html(result)     should include("You cannot use this service")
    }
  }
}
