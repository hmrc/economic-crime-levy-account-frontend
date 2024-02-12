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

import cats.data.EitherT
import org.mockito.ArgumentMatchers.any
import play.api.http.Status.OK
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsString, status}
import uk.gov.hmrc.economiccrimelevyaccount.{ValidFinancialDataResponse, ValidFinancialViewDetails}
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.models.errors.EclAccountError
import uk.gov.hmrc.economiccrimelevyaccount.services.EclAccountService
import uk.gov.hmrc.economiccrimelevyaccount.views.html.{NoPaymentsView, PaymentsView}

import scala.concurrent.Future

class ViewYourPaymentsControllerSpec extends SpecBase {

  val mockECLAccountService: EclAccountService = mock[EclAccountService]
  val paymentsView: PaymentsView               = app.injector.instanceOf[PaymentsView]
  val noPaymentsView: NoPaymentsView           = app.injector.instanceOf[NoPaymentsView]

  val controller = new ViewYourPaymentsController(
    mcc,
    fakeAuthorisedAction,
    mockECLAccountService,
    paymentsView,
    noPaymentsView,
    appConfig
  )

  "onPageLoad" should {
    "return OK and the correct view when financialData is present" in forAll {
      (financialData: ValidFinancialDataResponse, financialViewDetails: ValidFinancialViewDetails) =>
        when(mockECLAccountService.retrieveFinancialData(any()))
          .thenReturn(EitherT.rightT[Future, EclAccountError](Some(financialData.financialDataResponse)))

        when(mockECLAccountService.prepareFinancialDetails(any()))
          .thenReturn(EitherT.rightT[Future, EclAccountError](Some(financialViewDetails.financialViewDetails)))

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)
        status(result)          shouldBe OK
        contentAsString(result) shouldBe paymentsView(
          financialViewDetails.financialViewDetails,
          appConfig
        )(fakeRequest, messages)
          .toString()
    }
    "return OK and the correct view when financialData is missing" in {
      when(mockECLAccountService.retrieveFinancialData(any()))
        .thenReturn(EitherT.rightT[Future, EclAccountError](None))

      when(mockECLAccountService.prepareFinancialDetails(any()))
        .thenReturn(EitherT.rightT[Future, EclAccountError](None))

      val result: Future[Result] = controller.onPageLoad()(fakeRequest)
      status(result)          shouldBe OK
      contentAsString(result) shouldBe noPaymentsView()(fakeRequest, messages).toString()
    }
  }
}
