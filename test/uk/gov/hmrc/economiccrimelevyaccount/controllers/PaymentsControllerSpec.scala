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
import play.api.http.Status.CREATED
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.connectors.OpsJourneyError
import uk.gov.hmrc.economiccrimelevyaccount.models.{FinancialData, FinancialDetails, OpsJourneyResponse}
import uk.gov.hmrc.economiccrimelevyaccount.services.{ECLAccountService, OpsService}
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.PaymentType.StandardPayment

import java.time.LocalDate
import scala.concurrent.Future

class PaymentsControllerSpec extends SpecBase {

  val mockOpsService: OpsService                  = mock[OpsService]
  val mockFinancialDataService: ECLAccountService = mock[ECLAccountService]

  val date                = LocalDate.now()
  val expectedUrl: String = "http://www.bbc.co.uk"
  val opsJourneyError     = OpsJourneyError(
    CREATED,
    "Invalid Json"
  )

  val controller = new PaymentsController(
    mcc,
    fakeAuthorisedAction,
    mockFinancialDataService,
    mockOpsService
  )

  "onPageLoad" should {
    "redirect to URL if charge to pay" in {
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
            ArgumentMatchers.eq(amount)
          )(any())
        )
          .thenReturn(Future.successful(Left(opsJourneyResponse)))

        when(mockFinancialDataService.retrieveFinancialData(any()))
          .thenReturn(Future.successful(Some(response)))

        when(
          mockFinancialDataService.getLatestFinancialObligation(
            ArgumentMatchers.eq(response)
          )
        ).thenReturn(
          Some(
            FinancialDetails(
              amount,
              date,
              date,
              "",
              chargeReference,
              StandardPayment
            )
          )
        )

        val result = await(controller.onPageLoad()(fakeRequest))

        result shouldBe Redirect(expectedUrl)
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
            ArgumentMatchers.eq(amount)
          )(any())
        )
          .thenReturn(Future.successful(Right(opsJourneyError)))

        when(mockFinancialDataService.retrieveFinancialData(any()))
          .thenReturn(Future.successful(Some(response)))

        when(
          mockFinancialDataService.getLatestFinancialObligation(
            ArgumentMatchers.eq(response)
          )
        ).thenReturn(None)

        val result = await(controller.onPageLoad()(fakeRequest))

        result shouldBe Redirect(routes.AccountController.onPageLoad())
    }
  }
}
