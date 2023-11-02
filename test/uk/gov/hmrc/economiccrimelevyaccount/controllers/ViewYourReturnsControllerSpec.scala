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
import play.api.http.Status.{BAD_REQUEST, INTERNAL_SERVER_ERROR, OK}
import play.api.mvc.Result
import play.api.test.Helpers.{contentAsString, status}
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.connectors.ECLAccountConnector
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.ReturnStatus.{Due, Overdue, Submitted}
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.ReturnsOverview
import uk.gov.hmrc.economiccrimelevyaccount.views.html.{NoReturnsView, ReturnsView}
import uk.gov.hmrc.economiccrimelevyaccount._
import uk.gov.hmrc.http.UpstreamErrorResponse

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class ViewYourReturnsControllerSpec extends SpecBase {

  val returnsView: ReturnsView                             = app.injector.instanceOf[ReturnsView]
  val noReturnsView: NoReturnsView                         = app.injector.instanceOf[NoReturnsView]
  val mockObligationDataConnector: ObligationDataConnector = mock[ObligationDataConnector]
  val mockFinancialDataConnector: ECLAccountConnector      = mock[ECLAccountConnector]

  val controller = new ViewYourReturnsController(
    mcc,
    fakeAuthorisedAction,
    mockObligationDataConnector,
    mockFinancialDataConnector,
    returnsView,
    noReturnsView
  )

  "onPageLoad" should {
    "return OK and the correct view when return is Due" in forAll {
      (obligationData: ObligationDataWithObligation, financialData: ValidFinancialDataResponse) =>
        when(
          mockObligationDataConnector.getObligationData()(any())
        ).thenReturn(Future.successful(Some(obligationData.obligationData)))

        when(
          mockFinancialDataConnector.getFinancialData()(any())
        ).thenReturn(Future.successful(Some(financialData.financialDataResponse)))

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
        when(
          mockObligationDataConnector.getObligationData()(any())
        ).thenReturn(Future.successful(Some(obligationData.obligationData)))

        when(
          mockFinancialDataConnector.getFinancialData()(any())
        ).thenReturn(Future.successful(Some(financialData.financialDataResponse)))

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
        when(
          mockObligationDataConnector.getObligationData()(any())
        ).thenReturn(Future.successful(Some(obligationData.obligationData)))

        when(
          mockFinancialDataConnector.getFinancialData()(any())
        ).thenReturn(Future.successful(Some(financialData.financialDataResponse)))

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)
        val dueReturns             = Seq(
          ReturnsOverview(
            "2022-2023",
            obligationData.obligationData.obligations.head.obligationDetails.head.inboundCorrespondenceDueDate,
            Submitted,
            "21XY",
            Some(eclRegistrationReference)
          )
        )
        status(result)          shouldBe OK
        contentAsString(result) shouldBe returnsView(
          dueReturns
        )(fakeRequest, messages).toString()
    }

    "return OK and the correct view when user has no returns" in {
      when(
        mockObligationDataConnector.getObligationData()(any())
      ).thenReturn(Future.successful(None))

      val result: Future[Result] = controller.onPageLoad()(fakeRequest)

      status(result)          shouldBe OK
      contentAsString(result) shouldBe noReturnsView()(fakeRequest, messages).toString()
    }

    "return INTERNAL_SERVER_ERROR when return is Submitted but no charge reference" in forAll { //TODO - is this the right status to return?
      (obligationData: ObligationDataWithSubmittedObligation, financialData: ValidFinancialDataResponse) =>
        when(
          mockObligationDataConnector.getObligationData()(any())
        ).thenReturn(Future.successful(Some(obligationData.obligationData)))

        when(
          mockFinancialDataConnector.getFinancialData()(any())
        ).thenReturn(
          Future.successful(
            Some(
              financialData.financialDataResponse.copy(
                documentDetails = None
              )
            )
          )
        )

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)
        status(result) shouldBe INTERNAL_SERVER_ERROR
    }

    "return INTERNAL_SERVER_ERROR when financial API fails" in forAll {
      (obligationData: ObligationDataWithSubmittedObligation) =>
        when(
          mockObligationDataConnector.getObligationData()(any())
        ).thenReturn(Future.successful(Some(obligationData.obligationData)))

        when(
          mockFinancialDataConnector.getFinancialData()(any())
        ).thenReturn(Future.failed(UpstreamErrorResponse("error response", BAD_REQUEST)))

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)
        status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }
}
