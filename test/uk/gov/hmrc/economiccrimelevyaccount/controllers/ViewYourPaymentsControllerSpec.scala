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

import org.mockito.ArgumentMatchers.any
import play.api.http.Status.OK
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsString, status}
import uk.gov.hmrc.economiccrimelevyaccount.ValidFinancialViewDetails
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyaccount.services.FinancialDataService
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.FinancialViewDetails
import uk.gov.hmrc.economiccrimelevyaccount.views.html.{NoPaymentsView, PaymentsView}

import scala.concurrent.Future

class ViewYourPaymentsControllerSpec extends SpecBase {

  val mockFinancialDataService: FinancialDataService = mock[FinancialDataService]
  val paymentsView                                   = app.injector.instanceOf[PaymentsView]
  val noPaymentsView                                 = app.injector.instanceOf[NoPaymentsView]

  val controller = new ViewYourPaymentsController(
    mcc,
    fakeAuthorisedAction,
    mockFinancialDataService,
    paymentsView,
    noPaymentsView,
    appConfig
  )

  "onPageLoad" should {
    "return OK and the correct view when financialData is present" in forAll {
      (financialViewDetails: ValidFinancialViewDetails) =>
        when(mockFinancialDataService.getFinancialDetails(any()))
          .thenReturn(Future.successful(Some(financialViewDetails.financialViewDetails)))

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)
        status(result)          shouldBe OK
        contentAsString(result) shouldBe paymentsView(
          financialViewDetails.financialViewDetails,
          appConfig.refundBaseUrl
        )(fakeRequest, messages)
          .toString()
    }
    "return OK and the correct view when financialData is missing" in {
      when(mockFinancialDataService.getFinancialDetails(any()))
        .thenReturn(Future.successful(None))

      val result: Future[Result] = controller.onPageLoad()(fakeRequest)
      status(result)          shouldBe OK
      contentAsString(result) shouldBe noPaymentsView()(fakeRequest, messages).toString()
    }
  }
}
