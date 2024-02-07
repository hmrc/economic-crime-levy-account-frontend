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
import play.api.mvc.Result
import play.api.test.Helpers._
import uk.gov.hmrc.economiccrimelevyaccount._
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.models.errors.{AuditError, EclAccountError, EnrolmentStoreError}
import uk.gov.hmrc.economiccrimelevyaccount.models.{EclReference, FinancialData}
import uk.gov.hmrc.economiccrimelevyaccount.services.{AuditService, EclAccountService, EnrolmentStoreProxyService}
import uk.gov.hmrc.economiccrimelevyaccount.views.html.AccountView
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import java.time.LocalDate
import scala.concurrent.Future

class EclAccountControllerSpec extends SpecBase {

  val mockEnrolmentStoreProxyService: EnrolmentStoreProxyService = mock[EnrolmentStoreProxyService]
  val mockECLAccountService: EclAccountService                   = mock[EclAccountService]
  val mockAuditService: AuditService                             = mock[AuditService]

  val view: AccountView = app.injector.instanceOf[AccountView]

  val controller = new AccountController(
    mcc,
    fakeAuthorisedAction,
    mockEnrolmentStoreProxyService,
    view,
    mockECLAccountService,
    mockAuditService
  )

  "onPageLoad" should {
    "return OK and the correct view when obligationData and financialData is present" in forAll {
      (
        eclRegistrationDate: LocalDate,
        obligationData: ObligationDataWithObligation,
        validFinancialDataResponse: ValidFinancialDataResponse
      ) =>
        when(
          mockEnrolmentStoreProxyService.getEclRegistrationDate(any[String].asInstanceOf[EclReference])(any())
        ).thenReturn(EitherT.rightT[Future, EnrolmentStoreError](eclRegistrationDate))

        when(mockECLAccountService.retrieveObligationData(any()))
          .thenReturn(EitherT.rightT[Future, EclAccountError](Some(obligationData.obligationData)))

        when(mockAuditService.auditAccountViewed(any(), any[String].asInstanceOf[EclReference], any(), any())(any()))
          .thenReturn(EitherT.rightT[Future, AuditError](AuditResult.Success))

        when(mockECLAccountService.retrieveFinancialData(any()))
          .thenReturn(EitherT.rightT[Future, EclAccountError](Some(validFinancialDataResponse.financialDataResponse)))

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        status(result) shouldBe OK

    }

    "return OK and correct view when ObligationData is not present" in forAll { (eclRegistrationDate: LocalDate) =>
      when(
        mockEnrolmentStoreProxyService.getEclRegistrationDate(
          any[String].asInstanceOf[EclReference]
        )(any())
      ).thenReturn(EitherT.rightT[Future, EnrolmentStoreError](eclRegistrationDate))

      when(mockECLAccountService.retrieveObligationData(any()))
        .thenReturn(EitherT.rightT[Future, EclAccountError](None))

      when(mockAuditService.auditAccountViewed(any(), any[String].asInstanceOf[EclReference], any(), any())(any()))
        .thenReturn(EitherT.rightT[Future, AuditError](AuditResult.Success))

      val result: Future[Result] = controller.onPageLoad()(requestWithEclReference)

      status(result) shouldBe OK

    }

    "return OK and correct view when ObligationData is present but it's Overdue" in forAll {
      (
        eclRegistrationDate: LocalDate,
        overdueObligationData: ObligationDataWithOverdueObligation
      ) =>
        when(
          mockEnrolmentStoreProxyService.getEclRegistrationDate(any[String].asInstanceOf[EclReference])(any())
        ).thenReturn(EitherT.rightT[Future, EnrolmentStoreError](eclRegistrationDate))

        when(mockECLAccountService.retrieveObligationData(any()))
          .thenReturn(EitherT.rightT[Future, EclAccountError](Some(overdueObligationData.obligationData)))

        when(mockAuditService.auditAccountViewed(any(), any[String].asInstanceOf[EclReference], any(), any())(any()))
          .thenReturn(EitherT.rightT[Future, AuditError](AuditResult.Success))

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        status(result) shouldBe OK
    }

    "return OK and correct view when ObligationData is present but it's Submitted" in forAll {
      (
        eclRegistrationDate: LocalDate,
        submittedObligationData: ObligationDataWithSubmittedObligation
      ) =>
        when(
          mockEnrolmentStoreProxyService.getEclRegistrationDate(any[String].asInstanceOf[EclReference])(any())
        ).thenReturn(EitherT.rightT[Future, EnrolmentStoreError](eclRegistrationDate))

        when(mockECLAccountService.retrieveObligationData(any()))
          .thenReturn(EitherT.rightT[Future, EclAccountError](Some(submittedObligationData.obligationData)))

        when(mockAuditService.auditAccountViewed(any(), any[String].asInstanceOf[EclReference], any(), any())(any()))
          .thenReturn(EitherT.rightT[Future, AuditError](AuditResult.Success))

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        status(result) shouldBe OK
    }

    "return OK and the correct view when obligationData is present and financialData is not present" in forAll {
      (
        eclRegistrationDate: LocalDate,
        obligationData: ObligationDataWithObligation,
        validFinancialDataResponse: ValidFinancialDataResponse
      ) =>
        val invalidFinancialDataResponse =
          validFinancialDataResponse.copy(financialDataResponse = FinancialData(None, None))

        when(
          mockEnrolmentStoreProxyService.getEclRegistrationDate(any[String].asInstanceOf[EclReference])(any())
        ).thenReturn(EitherT.rightT[Future, EnrolmentStoreError](eclRegistrationDate))

        when(mockECLAccountService.retrieveObligationData(any()))
          .thenReturn(EitherT.rightT[Future, EclAccountError](Some(obligationData.obligationData)))

        when(mockAuditService.auditAccountViewed(any(), any[String].asInstanceOf[EclReference], any(), any())(any()))
          .thenReturn(EitherT.rightT[Future, AuditError](AuditResult.Success))

        when(mockECLAccountService.retrieveFinancialData(any()))
          .thenReturn(EitherT.rightT[Future, EclAccountError](Some(invalidFinancialDataResponse.financialDataResponse)))

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        status(result) shouldBe OK
    }
  }

}
