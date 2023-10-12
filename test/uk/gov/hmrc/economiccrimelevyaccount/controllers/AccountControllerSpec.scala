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
import uk.gov.hmrc.economiccrimelevyaccount.models.{FinancialDataResponse, FinancialDetails}
import uk.gov.hmrc.economiccrimelevyaccount.services.{EnrolmentStoreProxyService, FinancialDataService}
import uk.gov.hmrc.economiccrimelevyaccount.views.ViewUtils
import uk.gov.hmrc.economiccrimelevyaccount.views.html.AccountView
import uk.gov.hmrc.economiccrimelevyaccount._
import uk.gov.hmrc.play.audit.http.connector.{AuditConnector, AuditResult}

import java.time.LocalDate
import scala.concurrent.Future

class AccountControllerSpec extends SpecBase {

  val mockEnrolmentStoreProxyService: EnrolmentStoreProxyService = mock[EnrolmentStoreProxyService]
  val mockFinancialDataService: FinancialDataService             = mock[FinancialDataService]
  val mockObligationDataConnector: ObligationDataConnector       = mock[ObligationDataConnector]
  val mockAuditConnector: AuditConnector                         = mock[AuditConnector]

  val view: AccountView = app.injector.instanceOf[AccountView]

  val controller = new AccountController(
    mcc,
    fakeAuthorisedAction,
    mockEnrolmentStoreProxyService,
    view,
    mockObligationDataConnector,
    mockFinancialDataService,
    mockAuditConnector
  )

  "onPageLoad" should {
    "return OK and the correct view when obligationData and financialData is present" in forAll {
      (
        eclRegistrationDate: LocalDate,
        obligationData: ObligationDataWithObligation,
        financialDetails: FinancialDetails,
        validFinancialDataResponse: ValidFinancialDataResponse
      ) =>
        val validFinancialDetails = financialDetails.copy(amount = BigDecimal("10000"))
        when(
          mockEnrolmentStoreProxyService.getEclRegistrationDate(ArgumentMatchers.eq(eclRegistrationReference))(any())
        )
          .thenReturn(Future.successful(eclRegistrationDate))

        when(mockObligationDataConnector.getObligationData()(any()))
          .thenReturn(Future.successful(Some(obligationData.obligationData)))

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(AuditResult.Success))

        when(mockFinancialDataService.getLatestFinancialObligation(any()))
          .thenReturn(Some(validFinancialDetails))

        when(mockFinancialDataService.retrieveFinancialData(any()))
          .thenReturn(Future.successful(Some(validFinancialDataResponse.financialDataResponse)))

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        status(result)          shouldBe OK
        contentAsString(result) shouldBe view(
          eclRegistrationReference,
          ViewUtils.formatLocalDate(eclRegistrationDate)(messages),
          Some(obligationData.obligationData.obligations.head.obligationDetails.head),
          Some(validFinancialDetails)
        )(fakeRequest, messages).toString

        verify(mockAuditConnector, times(1)).sendExtendedEvent(any())(any(), any())

        reset(mockAuditConnector)
    }

    "return OK and correct view when ObligationData is not present" in forAll {
      (eclRegistrationDate: LocalDate, financialDetails: FinancialDetails) =>
        val validFinancialDetails = financialDetails.copy(amount = BigDecimal("10000"))

        when(
          mockEnrolmentStoreProxyService.getEclRegistrationDate(ArgumentMatchers.eq(eclRegistrationReference))(any())
        ).thenReturn(Future.successful(eclRegistrationDate))

        when(
          mockObligationDataConnector.getObligationData()(any())
        ).thenReturn(Future.successful(None))

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(AuditResult.Success))

        when(mockFinancialDataService.getLatestFinancialObligation(any()))
          .thenReturn(Some(validFinancialDetails))

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        status(result)          shouldBe OK
        contentAsString(result) shouldBe view(
          eclRegistrationReference,
          ViewUtils.formatLocalDate(eclRegistrationDate)(messages),
          None,
          Some(validFinancialDetails)
        )(fakeRequest, messages).toString()

        verify(mockAuditConnector, times(1)).sendExtendedEvent(any())(any(), any())

        reset(mockAuditConnector)
    }

    "return OK and correct view when ObligationData is present but it's Overdue" in forAll {
      (
        eclRegistrationDate: LocalDate,
        overdueObligationData: ObligationDataWithOverdueObligation,
        financialDetails: FinancialDetails
      ) =>
        val validFinancialDetails = financialDetails.copy(amount = BigDecimal("10000"))

        when(
          mockEnrolmentStoreProxyService.getEclRegistrationDate(ArgumentMatchers.eq(eclRegistrationReference))(any())
        ).thenReturn(Future.successful(eclRegistrationDate))

        when(
          mockObligationDataConnector.getObligationData()(any())
        ).thenReturn(Future.successful(Some(overdueObligationData.obligationData)))

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(AuditResult.Success))

        when(mockFinancialDataService.getLatestFinancialObligation(any()))
          .thenReturn(Some(validFinancialDetails))

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        status(result)          shouldBe OK
        contentAsString(result) shouldBe view(
          eclRegistrationReference,
          ViewUtils.formatLocalDate(eclRegistrationDate)(messages),
          Some(overdueObligationData.obligationData.obligations.head.obligationDetails.head),
          Some(validFinancialDetails)
        )(fakeRequest, messages).toString()

        verify(mockAuditConnector, times(1)).sendExtendedEvent(any())(any(), any())

        reset(mockAuditConnector)
    }

    "return OK and correct view when ObligationData is present but it's Submitted" in forAll {
      (
        eclRegistrationDate: LocalDate,
        submittedObligationData: ObligationDataWithSubmittedObligation,
        financialDetails: FinancialDetails
      ) =>
        val validFinancialDetails = financialDetails.copy(amount = BigDecimal("10000"))

        when(
          mockEnrolmentStoreProxyService.getEclRegistrationDate(ArgumentMatchers.eq(eclRegistrationReference))(any())
        ).thenReturn(Future.successful(eclRegistrationDate))

        when(
          mockObligationDataConnector.getObligationData()(any())
        ).thenReturn(Future.successful(Some(submittedObligationData.obligationData)))

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(AuditResult.Success))

        when(mockFinancialDataService.getLatestFinancialObligation(any()))
          .thenReturn(Some(validFinancialDetails))

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        status(result)          shouldBe OK
        contentAsString(result) shouldBe view(
          eclRegistrationReference,
          ViewUtils.formatLocalDate(eclRegistrationDate)(messages),
          None,
          Some(validFinancialDetails)
        )(fakeRequest, messages).toString()

        verify(mockAuditConnector, times(1)).sendExtendedEvent(any())(any(), any())

        reset(mockAuditConnector)
    }

    "return OK and the correct view when obligationData is present and financialData is not present" in forAll {
      (
        eclRegistrationDate: LocalDate,
        obligationData: ObligationDataWithObligation,
        validFinancialDataResponse: ValidFinancialDataResponse
      ) =>
        val invalidFinancialDataResponse =
          validFinancialDataResponse.copy(financialDataResponse = FinancialDataResponse(None, None))

        when(
          mockEnrolmentStoreProxyService.getEclRegistrationDate(ArgumentMatchers.eq(eclRegistrationReference))(any())
        )
          .thenReturn(Future.successful(eclRegistrationDate))

        when(mockObligationDataConnector.getObligationData()(any()))
          .thenReturn(Future.successful(Some(obligationData.obligationData)))

        when(mockAuditConnector.sendExtendedEvent(any())(any(), any()))
          .thenReturn(Future.successful(AuditResult.Success))

        when(mockFinancialDataService.retrieveFinancialData(any()))
          .thenReturn(Future.successful(Some(invalidFinancialDataResponse.financialDataResponse)))

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        status(result)          shouldBe OK
        contentAsString(result) shouldBe view(
          eclRegistrationReference,
          ViewUtils.formatLocalDate(eclRegistrationDate)(messages),
          Some(obligationData.obligationData.obligations.head.obligationDetails.head),
          None
        )(fakeRequest, messages).toString

        verify(mockAuditConnector, times(1)).sendExtendedEvent(any())(any(), any())

        reset(mockAuditConnector)
    }
  }

}
