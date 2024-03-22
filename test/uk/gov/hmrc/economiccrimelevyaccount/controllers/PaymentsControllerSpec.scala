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
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.http.Status.{INTERNAL_SERVER_ERROR, SEE_OTHER}
import play.api.test.Helpers.status
import uk.gov.hmrc.economiccrimelevyaccount.ValidFinancialDataResponse
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.models.errors.{EclAccountError, OpsError}
import uk.gov.hmrc.economiccrimelevyaccount.models.{FinancialData, OpsJourneyResponse, Payment}
import uk.gov.hmrc.economiccrimelevyaccount.services.{EclAccountService, OpsService}

import java.time.LocalDate
import scala.concurrent.Future

class PaymentsControllerSpec extends SpecBase {

  val mockOpsService: OpsService               = mock[OpsService]
  val mockECLAccountService: EclAccountService = mock[EclAccountService]

  val date: LocalDate     = LocalDate.now()
  val expectedUrl: String = "http://test-url.co.uk"

  val controller = new PaymentsController(
    mcc,
    fakeAuthorisedAction,
    mockECLAccountService,
    mockOpsService
  )

  "onPageLoad" should {
    "redirect to URL if charge to pay with charge reference" in {
      (
        chargeReference: String,
        amount: BigDecimal
      ) =>
        val opsJourneyResponse = OpsJourneyResponse(
          "",
          expectedUrl
        )

        val response = FinancialData(
          None,
          None
        )

        when(
          mockOpsService.startOpsJourney(
            ArgumentMatchers.eq(chargeReference),
            ArgumentMatchers.eq(amount),
            any()
          )(any())
        ).thenReturn(EitherT.rightT[Future, OpsError](opsJourneyResponse))

        when(mockECLAccountService.retrieveFinancialData(any()))
          .thenReturn(EitherT.rightT[Future, EclAccountError](Some(response)))

        val result = await(controller.onPageLoad(Some(chargeReference))(fakeRequest))

        result shouldBe Redirect(expectedUrl)

        reset(mockOpsService)
        reset(mockECLAccountService)
    }

    "redirect to URL if charge to pay with no charge reference" in {
      (
        chargeReference: String,
        amount: BigDecimal
      ) =>
        val opsJourneyResponse = OpsJourneyResponse(
          "",
          expectedUrl
        )

        val response = FinancialData(
          None,
          None
        )

        when(
          mockOpsService.startOpsJourney(
            ArgumentMatchers.eq(chargeReference),
            ArgumentMatchers.eq(amount),
            any()
          )(any())
        ).thenReturn(EitherT.rightT[Future, OpsError](opsJourneyResponse))

        when(mockECLAccountService.retrieveFinancialData(any()))
          .thenReturn(EitherT.rightT[Future, EclAccountError](Some(response)))

        val result = await(controller.onPageLoad(None)(fakeRequest))

        result shouldBe Redirect(expectedUrl)

        reset(mockOpsService)
        reset(mockECLAccountService)
    }

    "redirect to account page if no data" in {
      (
        chargeReference: String,
        amount: BigDecimal
      ) =>
        val response = FinancialData(
          None,
          None
        )

        when(
          mockOpsService.startOpsJourney(
            ArgumentMatchers.eq(chargeReference),
            ArgumentMatchers.eq(amount),
            any()
          )(any())
        ).thenReturn(
          EitherT.leftT[Future, OpsJourneyResponse](OpsError.BadGateway("Internal server error", INTERNAL_SERVER_ERROR))
        )

        when(mockECLAccountService.retrieveFinancialData(any()))
          .thenReturn(EitherT.rightT[Future, EclAccountError](Some(response)))

        val result = await(controller.onPageLoad(Some(chargeReference))(fakeRequest))

        result shouldBe Redirect(routes.AccountController.onPageLoad())

        reset(mockOpsService)
        reset(mockECLAccountService)
    }

    "return INTERNAL_SERVER_ERROR when document type is Payment" in forAll {
      (
        chargeReference: String,
        financialData: ValidFinancialDataResponse
      ) =>
        val documentDetails =
          financialData.financialDataResponse.documentDetails.get(0).copy(documentType = Some(Payment))

        val updatedResponse = financialData.financialDataResponse.copy(documentDetails = Some(Seq(documentDetails)))

        when(mockECLAccountService.retrieveFinancialData(any()))
          .thenReturn(EitherT.rightT[Future, EclAccountError](Some(updatedResponse)))

        val result = controller.onPageLoad(Some(chargeReference))(fakeRequest)

        status(result) shouldBe INTERNAL_SERVER_ERROR

        reset(mockECLAccountService)
    }

    "return INTERNAL_SERVER_ERROR when ETMP didn't return financial data" in forAll {
      (
        chargeReference: String,
      ) =>
        when(mockECLAccountService.retrieveFinancialData(any()))
          .thenReturn(EitherT.rightT[Future, EclAccountError](None))

        val result = controller.onPageLoad(Some(chargeReference))(fakeRequest)

        status(result) shouldBe SEE_OTHER
        reset(mockECLAccountService)
    }
  }
}
