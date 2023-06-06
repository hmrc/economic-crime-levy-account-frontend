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
import uk.gov.hmrc.economiccrimelevyaccount.{ObligationDataWithObligation, ObligationDataWithOverdueObligation, ObligationDataWithSubmittedObligation}
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.connectors.ObligationDataConnector
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.ReturnsOverview
import uk.gov.hmrc.economiccrimelevyaccount.views.html.{NoReturnsView, ReturnsView}
import scala.concurrent.Future

class ViewYourReturnsControllerSpec extends SpecBase {

  val returnsView: ReturnsView                             = app.injector.instanceOf[ReturnsView]
  val noReturnsView: NoReturnsView                         = app.injector.instanceOf[NoReturnsView]
  val mockObligationDataConnector: ObligationDataConnector = mock[ObligationDataConnector]

  val controller = new ViewYourReturnsController(
    mcc,
    fakeAuthorisedAction,
    mockObligationDataConnector,
    returnsView,
    noReturnsView
  )

  "onPageLoad" should {
    "return OK and the correct view when return is Due" in forAll { (obligationData: ObligationDataWithObligation) =>
      when(
        mockObligationDataConnector.getObligationData()(any())
      ).thenReturn(Future.successful(Some(obligationData.obligationData)))

      val result: Future[Result] = controller.onPageLoad()(fakeRequest)
      val dueReturns             = Seq(
        ReturnsOverview(
          "2022-2023",
          obligationData.obligationData.obligations.head.obligationDetails.head.inboundCorrespondenceDueDate,
          "DUE",
          "period-key",
          eclRegistrationReference
        )
      )
      status(result)          shouldBe OK
      contentAsString(result) shouldBe returnsView(
        dueReturns
      )(fakeRequest, messages).toString()
    }

    "return OK and the correct view when return is Overdue" in forAll {
      (obligationData: ObligationDataWithOverdueObligation) =>
        when(
          mockObligationDataConnector.getObligationData()(any())
        ).thenReturn(Future.successful(Some(obligationData.obligationData)))

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)
        val dueReturns             = Seq(
          ReturnsOverview(
            "2022-2023",
            obligationData.obligationData.obligations.head.obligationDetails.head.inboundCorrespondenceDueDate,
            "OVERDUE",
            "period-key",
            eclRegistrationReference
          )
        )
        status(result)          shouldBe OK
        contentAsString(result) shouldBe returnsView(
          dueReturns
        )(fakeRequest, messages).toString()
    }

    "return OK and the correct view when return is Submitted" in forAll {
      (obligationData: ObligationDataWithSubmittedObligation) =>
        when(
          mockObligationDataConnector.getObligationData()(any())
        ).thenReturn(Future.successful(Some(obligationData.obligationData)))

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)
        val dueReturns             = Seq(
          ReturnsOverview(
            "2022-2023",
            obligationData.obligationData.obligations.head.obligationDetails.head.inboundCorrespondenceDueDate,
            "SUBMITTED",
            "period-key",
            eclRegistrationReference
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
  }
}
