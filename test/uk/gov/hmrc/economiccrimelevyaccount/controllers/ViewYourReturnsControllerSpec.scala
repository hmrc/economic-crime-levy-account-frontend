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
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsString, status}
import uk.gov.hmrc.economiccrimelevyaccount._
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.models.FinancialData
import uk.gov.hmrc.economiccrimelevyaccount.models.errors.{EclAccountError, EclRegistrationError}
import uk.gov.hmrc.economiccrimelevyaccount.services.{EclAccountService, EclRegistrationService}
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.ReturnStatus.{Due, Overdue, Submitted}
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.{ReturnsOverview, ReturnsViewModel}
import uk.gov.hmrc.economiccrimelevyaccount.views.html.{NoReturnsView, ReturnsView}

import java.time.LocalDate
import scala.concurrent.Future

class ViewYourReturnsControllerSpec extends SpecBase {

  val returnsView: ReturnsView                           = app.injector.instanceOf[ReturnsView]
  val noReturnsView: NoReturnsView                       = app.injector.instanceOf[NoReturnsView]
  val mockEclAccountService: EclAccountService           = mock[EclAccountService]
  val mockEclRegistrationService: EclRegistrationService = mock[EclRegistrationService]

  val controller = new ViewYourReturnsController(
    mcc,
    fakeAuthorisedAction,
    mockEclAccountService,
    returnsView,
    noReturnsView,
    mockEclRegistrationService
  )

  val financialYearStartYear = s"${LocalDate.now().minusYears(1).getYear}"
  val financialYearEndYear   = s"${LocalDate.now().getYear}"
  val fromTo                 = s"$financialYearStartYear-$financialYearEndYear"

  "onPageLoad" should {
    "return OK and the correct view when return is Due and financial data returned" in forAll {
      (obligationData: ObligationDataWithObligation, financialData: ValidFinancialDataResponse) =>
        when(
          mockEclAccountService.retrieveObligationData(any())
        ).thenReturn(EitherT.rightT[Future, EclAccountError](Some(obligationData.obligationData)))

        when(
          mockEclAccountService.retrieveFinancialData(any())
        ).thenReturn(EitherT.rightT[Future, EclAccountError](Some(financialData.financialDataResponse)))

        when(
          mockEclRegistrationService.getSubscriptionStatus(any())(any())
        ).thenReturn(EitherT.rightT[Future, EclRegistrationError](testSubscribedSubscriptionStatus))

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        val dueReturns = Seq(
          ReturnsOverview(
            fromTo,
            obligationData.obligationData.obligations.head.obligationDetails.head.inboundCorrespondenceDueDate,
            Due,
            "21XY",
            None
          )
        )

        val viewModel: ReturnsViewModel =
          ReturnsViewModel(dueReturns, testEclReference, testSubscribedSubscriptionStatus)

        status(result)          shouldBe OK
        contentAsString(result) shouldBe returnsView(
          viewModel
        )(fakeRequest, messages).toString()
    }

    "return OK and the correct view when return is Due and no financial data returned" in forAll {
      (obligationData: ObligationDataWithObligation) =>
        when(
          mockEclAccountService.retrieveObligationData(any())
        ).thenReturn(EitherT.rightT[Future, EclAccountError](Some(obligationData.obligationData)))

        when(
          mockEclAccountService.retrieveFinancialData(any())
        ).thenReturn(EitherT.rightT[Future, EclAccountError](None))

        when(
          mockEclRegistrationService.getSubscriptionStatus(any())(any())
        ).thenReturn(EitherT.rightT[Future, EclRegistrationError](testSubscribedSubscriptionStatus))

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        val dueReturns = Seq(
          ReturnsOverview(
            fromTo,
            obligationData.obligationData.obligations.head.obligationDetails.head.inboundCorrespondenceDueDate,
            Due,
            "21XY",
            None
          )
        )

        val viewModel: ReturnsViewModel =
          ReturnsViewModel(dueReturns, testEclReference, testSubscribedSubscriptionStatus)

        status(result)          shouldBe OK
        contentAsString(result) shouldBe returnsView(
          viewModel
        )(fakeRequest, messages).toString()
    }

    "return OK and the correct view when return is Overdue" in forAll {
      (obligationData: ObligationDataWithOverdueObligation, financialData: ValidFinancialDataResponse) =>
        when(mockEclAccountService.retrieveObligationData(any()))
          .thenReturn(EitherT.rightT[Future, EclAccountError](Some(obligationData.obligationData)))

        when(
          mockEclAccountService.retrieveFinancialData(any())
        ).thenReturn(EitherT.rightT[Future, EclAccountError](Some(financialData.financialDataResponse)))

        when(
          mockEclRegistrationService.getSubscriptionStatus(any())(any())
        ).thenReturn(EitherT.rightT[Future, EclRegistrationError](testSubscribedSubscriptionStatus))

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        val dueReturns = Seq(
          ReturnsOverview(
            fromTo,
            obligationData.obligationData.obligations.head.obligationDetails.head.inboundCorrespondenceDueDate,
            Overdue,
            "21XY",
            None
          )
        )

        val viewModel: ReturnsViewModel =
          ReturnsViewModel(dueReturns, testEclReference, testSubscribedSubscriptionStatus)

        status(result)          shouldBe OK
        contentAsString(result) shouldBe returnsView(
          viewModel
        )(fakeRequest, messages).toString()
    }

    "return OK and the correct view when return is Overdue and financial data returned" in forAll {
      (obligationData: ObligationDataWithOverdueObligation, financialData: ValidFinancialDataResponse) =>
        when(mockEclAccountService.retrieveObligationData(any()))
          .thenReturn(EitherT.rightT[Future, EclAccountError](Some(obligationData.obligationData)))

        when(
          mockEclAccountService.retrieveFinancialData(any())
        ).thenReturn(EitherT.rightT[Future, EclAccountError](Some(financialData.financialDataResponse)))

        when(
          mockEclRegistrationService.getSubscriptionStatus(any())(any())
        ).thenReturn(EitherT.rightT[Future, EclRegistrationError](testSubscribedSubscriptionStatus))

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        val dueReturns = Seq(
          ReturnsOverview(
            fromTo,
            obligationData.obligationData.obligations.head.obligationDetails.head.inboundCorrespondenceDueDate,
            Overdue,
            "21XY",
            None
          )
        )

        val viewModel: ReturnsViewModel =
          ReturnsViewModel(dueReturns, testEclReference, testSubscribedSubscriptionStatus)

        status(result)          shouldBe OK
        contentAsString(result) shouldBe returnsView(
          viewModel
        )(fakeRequest, messages).toString()
    }

    "return OK and the correct view when return is Overdue and no financial data returned" in forAll {
      (obligationData: ObligationDataWithOverdueObligation) =>
        when(mockEclAccountService.retrieveObligationData(any()))
          .thenReturn(EitherT.rightT[Future, EclAccountError](Some(obligationData.obligationData)))

        when(
          mockEclAccountService.retrieveFinancialData(any())
        ).thenReturn(EitherT.rightT[Future, EclAccountError](None))

        when(
          mockEclRegistrationService.getSubscriptionStatus(any())(any())
        ).thenReturn(EitherT.rightT[Future, EclRegistrationError](testSubscribedSubscriptionStatus))

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        val dueReturns = Seq(
          ReturnsOverview(
            fromTo,
            obligationData.obligationData.obligations.head.obligationDetails.head.inboundCorrespondenceDueDate,
            Overdue,
            "21XY",
            None
          )
        )

        val viewModel: ReturnsViewModel =
          ReturnsViewModel(dueReturns, testEclReference, testSubscribedSubscriptionStatus)

        status(result)          shouldBe OK
        contentAsString(result) shouldBe returnsView(
          viewModel
        )(fakeRequest, messages).toString()
    }

    "return OK and the correct view when return is Submitted and financial data returned" in forAll {
      (obligationData: ObligationDataWithSubmittedObligation, financialData: ValidFinancialDataResponse) =>
        when(mockEclAccountService.retrieveObligationData(any()))
          .thenReturn(EitherT.rightT[Future, EclAccountError](Some(obligationData.obligationData)))

        when(
          mockEclAccountService.retrieveFinancialData(any())
        ).thenReturn(EitherT.rightT[Future, EclAccountError](Some(financialData.financialDataResponse)))

        when(
          mockEclRegistrationService.getSubscriptionStatus(any())(any())
        ).thenReturn(EitherT.rightT[Future, EclRegistrationError](testSubscribedSubscriptionStatus))

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        val dueReturns = Seq(
          ReturnsOverview(
            fromTo,
            obligationData.obligationData.obligations.head.obligationDetails.head.inboundCorrespondenceDueDate,
            Submitted,
            "21XY",
            Some(testEclReference.value)
          )
        )

        val viewModel: ReturnsViewModel =
          ReturnsViewModel(dueReturns, testEclReference, testSubscribedSubscriptionStatus)

        status(result)          shouldBe OK
        contentAsString(result) shouldBe returnsView(
          viewModel
        )(fakeRequest, messages).toString()
    }

    "return OK and the correct view when return is Submitted and no financial data returned" in forAll {
      (obligationData: ObligationDataWithSubmittedObligation) =>
        when(mockEclAccountService.retrieveObligationData(any()))
          .thenReturn(EitherT.rightT[Future, EclAccountError](Some(obligationData.obligationData)))

        when(
          mockEclAccountService.retrieveFinancialData(any())
        ).thenReturn(EitherT.rightT[Future, EclAccountError](None))

        when(
          mockEclRegistrationService.getSubscriptionStatus(any())(any())
        ).thenReturn(EitherT.rightT[Future, EclRegistrationError](testSubscribedSubscriptionStatus))

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        val dueReturns = Seq(
          ReturnsOverview(
            fromTo,
            obligationData.obligationData.obligations.head.obligationDetails.head.inboundCorrespondenceDueDate,
            Submitted,
            "21XY",
            None
          )
        )

        val viewModel: ReturnsViewModel =
          ReturnsViewModel(dueReturns, testEclReference, testSubscribedSubscriptionStatus)

        status(result)          shouldBe OK
        contentAsString(result) shouldBe returnsView(
          viewModel
        )(fakeRequest, messages).toString()
    }

    "return OK and the correct view when user has no returns" in {
      when(mockEclAccountService.retrieveObligationData(any()))
        .thenReturn(EitherT.rightT[Future, EclAccountError](None))

      when(
        mockEclRegistrationService.getSubscriptionStatus(any())(any())
      ).thenReturn(EitherT.rightT[Future, EclRegistrationError](testSubscribedSubscriptionStatus))

      val result: Future[Result] = controller.onPageLoad()(fakeRequest)

      status(result)          shouldBe OK
      contentAsString(result) shouldBe noReturnsView()(fakeRequest, messages).toString()
    }

    "return INTERNAL_SERVER_ERROR when financial API fails" in forAll {
      (obligationData: ObligationDataWithSubmittedObligation) =>
        when(mockEclAccountService.retrieveObligationData(any()))
          .thenReturn(EitherT.rightT[Future, EclAccountError](Some(obligationData.obligationData)))

        when(
          mockEclAccountService.retrieveFinancialData(any())
        ).thenReturn(
          EitherT.leftT[Future, Option[FinancialData]](
            EclAccountError.BadGateway("Internal server error", INTERNAL_SERVER_ERROR)
          )
        )

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)
        status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
