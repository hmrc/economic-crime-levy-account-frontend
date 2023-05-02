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

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.connectors.ObligationDataConnector
import uk.gov.hmrc.economiccrimelevyaccount.generators.CachedArbitraries.arbObligationData
import uk.gov.hmrc.economiccrimelevyaccount.models.{ObligationData, ObligationDetails, Open}
import uk.gov.hmrc.economiccrimelevyaccount.services.EnrolmentStoreProxyService
import uk.gov.hmrc.economiccrimelevyaccount.views.ViewUtils
import uk.gov.hmrc.economiccrimelevyaccount.views.html.AccountView

import java.time.LocalDate
import scala.concurrent.Future

class AccountControllerSpec extends SpecBase {

  val mockEnrolmentStoreProxyService: EnrolmentStoreProxyService = mock[EnrolmentStoreProxyService]
  val mockObligationDataConnector: ObligationDataConnector       = mock[ObligationDataConnector]

  val view: AccountView = app.injector.instanceOf[AccountView]

  val controller = new AccountController(
    mcc,
    fakeAuthorisedAction,
    mockEnrolmentStoreProxyService,
    view,
    mockObligationDataConnector
  )

  "onPageLoad" should {
    "return OK and the correct view" in forAll { (eclRegistrationDate: LocalDate, obligationData: ObligationData) =>
      when(mockEnrolmentStoreProxyService.getEclRegistrationDate(ArgumentMatchers.eq(eclRegistrationReference))(any()))
        .thenReturn(Future.successful(eclRegistrationDate))
      when(mockObligationDataConnector.getObligationData()(any())).thenReturn(Future.successful(Some(obligationData)))

      val result: Future[Result] = controller.onPageLoad()(fakeRequest)
      val obligationDetails      =
        ObligationDetails(Open, LocalDate.now(), LocalDate.now(), Some(LocalDate.now()), LocalDate.now(), "period-key")

      status(result)          shouldBe OK
      contentAsString(result) shouldBe view(
        eclRegistrationReference,
        ViewUtils.formatLocalDate(eclRegistrationDate)(messages),
        Some(obligationDetails)
      )(fakeRequest, messages).toString
    }
  }

}
