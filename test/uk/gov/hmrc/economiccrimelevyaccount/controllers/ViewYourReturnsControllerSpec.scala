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
import play.api.http.Status.{BAD_GATEWAY, INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsString, status}
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.ReturnStatus.{Due, Overdue, Submitted}
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.ReturnsOverview
import uk.gov.hmrc.economiccrimelevyaccount.views.html.{NoReturnsView, ReturnsView}
import uk.gov.hmrc.economiccrimelevyaccount._
import uk.gov.hmrc.economiccrimelevyaccount.models.FinancialData
import uk.gov.hmrc.economiccrimelevyaccount.models.errors.ECLAccountError
import uk.gov.hmrc.economiccrimelevyaccount.services.ECLAccountService

import scala.concurrent.Future

class ViewYourReturnsControllerSpec extends SpecBase {

  val returnsView: ReturnsView                 = app.injector.instanceOf[ReturnsView]
  val noReturnsView: NoReturnsView             = app.injector.instanceOf[NoReturnsView]
  val mockECLAccountService: ECLAccountService = mock[ECLAccountService]

  val controller = new ViewYourReturnsController(
    mcc,
    fakeAuthorisedAction,
    mockECLAccountService,
    returnsView,
    noReturnsView
  )

  "onPageLoad" should {
    "return OK and the correct view when return is Due" in forAll {
      (obligationData: ObligationDataWithObligation, financialData: ValidFinancialDataResponse) =>
        when(
          mockECLAccountService.retrieveObligationData(any())
        ).thenReturn(EitherT.rightT[Future, ECLAccountError](Some(obligationData.obligationData)))

        when(
          mockECLAccountService.retrieveFinancialData(any())
        ).thenReturn(EitherT.rightT[Future, ECLAccountError](Some(financialData.financialDataResponse)))

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)
        val dueReturns             = Seq(
          ReturnsOverview(
            "2022-2023",
            obligationData.obligationData.obligations.head.obligationDetails.head.inboundCorrespondenceDueDate,
            Due,
            "21XY",
            None
          )
        )
        status(result)          shouldBe OK
        contentAsString(result) shouldBe returnsView(
          dueReturns
        )(fakeRequest, messages).toString()
    }

    "return OK and the correct view when return is Overdue" in forAll {
      (obligationData: ObligationDataWithOverdueObligation, financialData: ValidFinancialDataResponse) =>
        when(mockECLAccountService.retrieveObligationData(any()))
          .thenReturn(EitherT.rightT[Future, ECLAccountError](Some(obligationData.obligationData)))

        when(
          mockECLAccountService.retrieveFinancialData(any())
        ).thenReturn(EitherT.rightT[Future, ECLAccountError](Some(financialData.financialDataResponse)))

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)
        val dueReturns             = Seq(
          ReturnsOverview(
            "2022-2023",
            obligationData.obligationData.obligations.head.obligationDetails.head.inboundCorrespondenceDueDate,
            Overdue,
            "21XY",
            None
          )
        )
        status(result)          shouldBe OK
        contentAsString(result) shouldBe returnsView(
          dueReturns
        )(fakeRequest, messages).toString()
    }

    "return OK and the correct view when return is Submitted" in forAll {
      (obligationData: ObligationDataWithSubmittedObligation, financialData: ValidFinancialDataResponse) =>
        when(mockECLAccountService.retrieveObligationData(any()))
          .thenReturn(EitherT.rightT[Future, ECLAccountError](Some(obligationData.obligationData)))

        when(
          mockECLAccountService.retrieveFinancialData(any())
        ).thenReturn(EitherT.rightT[Future, ECLAccountError](Some(financialData.financialDataResponse)))

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)
        val dueReturns             = Seq(
          ReturnsOverview(
            "2022-2023",
            obligationData.obligationData.obligations.head.obligationDetails.head.inboundCorrespondenceDueDate,
            Submitted,
            "21XY",
            Some(eclRegistrationReference.value)
          )
        )
        status(result)          shouldBe OK
        contentAsString(result) shouldBe returnsView(
          dueReturns
        )(fakeRequest, messages).toString()
    }

    "return OK and the correct view when user has no returns" in {
      when(mockECLAccountService.retrieveObligationData(any()))
        .thenReturn(EitherT.rightT[Future, ECLAccountError](None))

      val result: Future[Result] = controller.onPageLoad()(fakeRequest)

      status(result)          shouldBe OK
      contentAsString(result) shouldBe noReturnsView()(fakeRequest, messages).toString()
    }

    "return BAD_GATEWAY when financial API fails" in forAll { (obligationData: ObligationDataWithSubmittedObligation) =>
      when(mockECLAccountService.retrieveObligationData(any()))
        .thenReturn(EitherT.rightT[Future, ECLAccountError](Some(obligationData.obligationData)))

      when(
        mockECLAccountService.retrieveFinancialData(any())
      ).thenReturn(
        EitherT.leftT[Future, Option[FinancialData]](
          ECLAccountError.BadGateway("Internal server error", INTERNAL_SERVER_ERROR)
        )
      )

      val result: Future[Result] = controller.onPageLoad()(fakeRequest)
      status(result) shouldBe BAD_GATEWAY
    }
  }
}
