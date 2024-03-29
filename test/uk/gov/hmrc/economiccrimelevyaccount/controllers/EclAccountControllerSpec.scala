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
import uk.gov.hmrc.economiccrimelevyaccount.models.errors.{AuditError, EclAccountError, EclRegistrationError}
import uk.gov.hmrc.economiccrimelevyaccount.models.{EclReference, FinancialData, ObligationData}
import uk.gov.hmrc.economiccrimelevyaccount.services.{AuditService, EclAccountService, EclRegistrationService}
import uk.gov.hmrc.economiccrimelevyaccount.views.html.AccountView
import uk.gov.hmrc.play.audit.http.connector.AuditResult

import java.time.LocalDate
import scala.concurrent.Future

class EclAccountControllerSpec extends SpecBase {

  val mockEclAccountService: EclAccountService           = mock[EclAccountService]
  val mockEclRegistrationService: EclRegistrationService = mock[EclRegistrationService]
  val mockAuditService: AuditService                     = mock[AuditService]

  val view: AccountView = app.injector.instanceOf[AccountView]

  val controller = new AccountController(
    mcc,
    fakeAuthorisedAction,
    view,
    mockEclAccountService,
    mockAuditService,
    mockEclRegistrationService,
    appConfig
  )

  "onPageLoad" should {
    "return OK and the correct view when obligationData and financialData is present" in forAll {
      (
        obligationData: ObligationDataWithObligation,
        validFinancialDataResponse: ValidFinancialDataResponse
      ) =>
        when(mockEclAccountService.retrieveObligationData(any()))
          .thenReturn(EitherT.rightT[Future, EclAccountError](Some(obligationData.obligationData)))

        when(mockAuditService.auditAccountViewed(any(), any[String].asInstanceOf[EclReference], any(), any())(any()))
          .thenReturn(EitherT.rightT[Future, AuditError](AuditResult.Success))

        when(mockEclAccountService.retrieveFinancialData(any()))
          .thenReturn(EitherT.rightT[Future, EclAccountError](Some(validFinancialDataResponse.financialDataResponse)))

        when(mockEclRegistrationService.getSubscriptionStatus(any())(any()))
          .thenReturn(EitherT.rightT[Future, EclRegistrationError](testSubscribedSubscriptionStatus))

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        status(result) shouldBe OK

    }

    "return OK and correct view when ObligationData is not present" in forAll { (eclRegistrationDate: LocalDate) =>
      when(mockEclAccountService.retrieveObligationData(any()))
        .thenReturn(EitherT.rightT[Future, EclAccountError](None))

      when(mockAuditService.auditAccountViewed(any(), any[String].asInstanceOf[EclReference], any(), any())(any()))
        .thenReturn(EitherT.rightT[Future, AuditError](AuditResult.Success))

      when(mockEclRegistrationService.getSubscriptionStatus(any())(any()))
        .thenReturn(EitherT.rightT[Future, EclRegistrationError](testSubscribedSubscriptionStatus))

      val result: Future[Result] = controller.onPageLoad()(requestWithEclReference)

      status(result) shouldBe OK

    }

    "return OK and correct view when ObligationData is present but it's Overdue" in forAll {
      (
        overdueObligationData: ObligationDataWithOverdueObligation
      ) =>
        when(mockEclAccountService.retrieveObligationData(any()))
          .thenReturn(EitherT.rightT[Future, EclAccountError](Some(overdueObligationData.obligationData)))

        when(mockAuditService.auditAccountViewed(any(), any[String].asInstanceOf[EclReference], any(), any())(any()))
          .thenReturn(EitherT.rightT[Future, AuditError](AuditResult.Success))

        when(mockEclRegistrationService.getSubscriptionStatus(any())(any()))
          .thenReturn(EitherT.rightT[Future, EclRegistrationError](testSubscribedSubscriptionStatus))

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        status(result) shouldBe OK
    }

    "return OK and correct view when ObligationData is present but it's Submitted" in forAll {
      (
        submittedObligationData: ObligationDataWithSubmittedObligation
      ) =>
        when(mockEclAccountService.retrieveObligationData(any()))
          .thenReturn(EitherT.rightT[Future, EclAccountError](Some(submittedObligationData.obligationData)))

        when(mockAuditService.auditAccountViewed(any(), any[String].asInstanceOf[EclReference], any(), any())(any()))
          .thenReturn(EitherT.rightT[Future, AuditError](AuditResult.Success))

        when(mockEclRegistrationService.getSubscriptionStatus(any())(any()))
          .thenReturn(EitherT.rightT[Future, EclRegistrationError](testSubscribedSubscriptionStatus))

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        status(result) shouldBe OK
    }

    "return OK and the correct view when obligationData is present and financialData is not present" in forAll {
      (
        obligationData: ObligationDataWithObligation,
        validFinancialDataResponse: ValidFinancialDataResponse
      ) =>
        when(mockEclAccountService.retrieveObligationData(any()))
          .thenReturn(EitherT.rightT[Future, EclAccountError](Some(obligationData.obligationData)))

        when(mockAuditService.auditAccountViewed(any(), any[String].asInstanceOf[EclReference], any(), any())(any()))
          .thenReturn(EitherT.rightT[Future, AuditError](AuditResult.Success))

        when(mockEclAccountService.retrieveFinancialData(any()))
          .thenReturn(EitherT.rightT[Future, EclAccountError](None))

        when(mockEclRegistrationService.getSubscriptionStatus(any())(any()))
          .thenReturn(EitherT.rightT[Future, EclRegistrationError](testSubscribedSubscriptionStatus))

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        status(result) shouldBe OK
    }

    "return INTERNAL_SERVER_ERROR when we receive error from eclAccount service for retrieveObligationData call" in forAll {
      (
        validFinancialDataResponse: ValidFinancialDataResponse
      ) =>
        val invalidFinancialDataResponse =
          validFinancialDataResponse.copy(financialDataResponse = FinancialData(None, None))

        when(mockEclAccountService.retrieveObligationData(any()))
          .thenReturn(
            EitherT.leftT[Future, Option[ObligationData]](
              EclAccountError.InternalUnexpectedError("Error message", Some(new Exception("Error message")))
            )
          )

        when(mockAuditService.auditAccountViewed(any(), any[String].asInstanceOf[EclReference], any(), any())(any()))
          .thenReturn(EitherT.rightT[Future, AuditError](AuditResult.Success))

        when(mockEclAccountService.retrieveFinancialData(any()))
          .thenReturn(EitherT.rightT[Future, EclAccountError](Some(invalidFinancialDataResponse.financialDataResponse)))

        when(mockEclRegistrationService.getSubscriptionStatus(any())(any()))
          .thenReturn(EitherT.rightT[Future, EclRegistrationError](testSubscribedSubscriptionStatus))

        val result: Future[Result] = controller.onPageLoad()(fakeRequest)

        status(result) shouldBe INTERNAL_SERVER_ERROR
    }
  }

}
