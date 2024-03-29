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

package uk.gov.hmrc.economiccrimelevyaccount.controllers

import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.views.html.AgentCannotAccessServiceView

import scala.concurrent.Future

class NotableErrorControllerSpec extends SpecBase {

  val agentCannotAccessServiceView: AgentCannotAccessServiceView = app.injector.instanceOf[AgentCannotAccessServiceView]

  val controller = new NotableErrorController(
    mcc,
    appConfig,
    agentCannotAccessServiceView
  )

  "notRegistered" should {
    "redirects to enrolment service and returns SEE_OTHER" in {
      val result: Future[Result] = controller.notRegistered()(fakeRequest)

      status(result) shouldBe SEE_OTHER
    }
  }

  "agentCannotAccessService" should {
    "return OK and the correct view" in {
      val result: Future[Result] = controller.agentCannotAccessService()(fakeRequest)

      status(result) shouldBe OK

      contentAsString(result) shouldBe agentCannotAccessServiceView()(fakeRequest, messages).toString
    }
  }

}
