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

package uk.gov.hmrc.economiccrimelevyaccount.connectors

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import org.scalatest.BeforeAndAfterEach
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyaccount.base.{OpsTestData, SpecBase}
import uk.gov.hmrc.economiccrimelevyaccount.models.{OpsJourneyRequest, OpsJourneyResponse, PaymentBlock}
import uk.gov.hmrc.http.{HttpClient, HttpResponse}

import scala.concurrent.Future

class OpsConnectorSpec extends SpecBase with OpsTestData {

  val mockHttpClient: HttpClient = mock[HttpClient]
  val connector                  = new OpsConnector(appConfig, mockHttpClient)
  val url                        = "http://google.co.uk"
  val expectedUrl: String        = "http://www.bbc.co.uk"

  "createJourney" should {

    "create an OPS journey successfully" in forAll { (chargeReference: String, amount: BigDecimal) =>
      val opsJourneyRequest = OpsJourneyRequest(
        chargeReference,
        amount * 100,
        url,
        url,
        None
      )

      val opsJourneyResponse = OpsJourneyResponse(
        "",
        expectedUrl
      )

      when(
        mockHttpClient.POST[OpsJourneyRequest, HttpResponse](
          any(),
          ArgumentMatchers.eq(opsJourneyRequest),
          any()
        )(
          any(),
          any(),
          any(),
          any()
        )
      ).thenReturn(
        Future.successful(
          HttpResponse(
            CREATED,
            Json.toJson[OpsJourneyResponse](opsJourneyResponse),
            Map()
          )
        )
      )

      val result = await(connector.createOpsJourney(opsJourneyRequest))

      result shouldBe Left(opsJourneyResponse)

      verify(mockHttpClient, times(1))
        .POST(
          any(),
          ArgumentMatchers.eq(opsJourneyRequest),
          any()
        )(
          any(),
          any(),
          any(),
          any()
        )

      reset(mockHttpClient)
    }

    "return an error if OPS journey creation fails" in forAll { (chargeReference: String, amount: BigDecimal) =>
      val opsJourneyRequest = OpsJourneyRequest(
        chargeReference,
        amount * 100,
        url,
        url,
        None
      )

      when(
        mockHttpClient.POST[OpsJourneyRequest, HttpResponse](
          any(),
          ArgumentMatchers.eq(opsJourneyRequest),
          any()
        )(
          any(),
          any(),
          any(),
          any()
        )
      ).thenReturn(
        Future.successful(
          HttpResponse(
            INTERNAL_SERVER_ERROR,
            "",
            Map()
          )
        )
      )

      val result = await(connector.createOpsJourney(opsJourneyRequest))

      result shouldBe Right(OpsApiError(INTERNAL_SERVER_ERROR, ""))

      verify(mockHttpClient, times(1))
        .POST(
          any(),
          ArgumentMatchers.eq(opsJourneyRequest),
          any()
        )(
          any(),
          any(),
          any(),
          any()
        )

      reset(mockHttpClient)
    }

    "return an error if invalid Json as journey info" in forAll { (chargeReference: String, amount: BigDecimal) =>
      val opsJourneyRequest = OpsJourneyRequest(
        chargeReference,
        amount * 100,
        url,
        url,
        None
      )

      when(
        mockHttpClient.POST[OpsJourneyRequest, HttpResponse](
          any(),
          ArgumentMatchers.eq(opsJourneyRequest),
          any()
        )(
          any(),
          any(),
          any(),
          any()
        )
      ).thenReturn(
        Future.successful(
          HttpResponse(
            CREATED,
            "{}",
            Map()
          )
        )
      )

      val result = await(connector.createOpsJourney(opsJourneyRequest))

      result shouldBe Right(OpsApiError(CREATED, "Invalid Json"))

      verify(mockHttpClient, times(1))
        .POST(
          any(),
          ArgumentMatchers.eq(opsJourneyRequest),
          any()
        )(
          any(),
          any(),
          any(),
          any()
        )

      reset(mockHttpClient)
    }

    "getPayments" should {

      "return a list of payments successfully" in forAll { (chargeReference: String) =>
        val paymentBlock = PaymentBlock(
          chargeReference,
          "",
          payments()
        )

        when(
          mockHttpClient.GET[HttpResponse](
            any(),
            any(),
            any()
          )(
            any(),
            any(),
            any()
          )
        ).thenReturn(
          Future.successful(
            HttpResponse(
              OK,
              Json.toJson[PaymentBlock](paymentBlock),
              Map()
            )
          )
        )

        val result = await(connector.getPayments(chargeReference))

        result shouldBe Left(paymentBlock.payments)

        verify(mockHttpClient, times(1))
          .GET(
            any(),
            any(),
            any()
          )(
            any(),
            any(),
            any()
          )

        reset(mockHttpClient)
      }

      "return an error if failed to get payments" in forAll { (chargeReference: String) =>
        when(
          mockHttpClient.GET[HttpResponse](
            any(),
            any(),
            any()
          )(
            any(),
            any(),
            any()
          )
        ).thenReturn(
          Future.successful(
            HttpResponse(
              INTERNAL_SERVER_ERROR,
              "",
              Map()
            )
          )
        )

        val result = await(connector.getPayments(chargeReference))

        result shouldBe Right(OpsApiError(INTERNAL_SERVER_ERROR, ""))

        verify(mockHttpClient, times(1))
          .GET(
            any(),
            any(),
            any()
          )(
            any(),
            any(),
            any()
          )

        reset(mockHttpClient)
      }
    }

    "return an error if invalid Json returned as payments" in forAll { (chargeReference: String) =>
      when(
        mockHttpClient.GET[HttpResponse](
          any(),
          any(),
          any()
        )(
          any(),
          any(),
          any()
        )
      ).thenReturn(
        Future.successful(
          HttpResponse(
            OK,
            "{}",
            Map()
          )
        )
      )

      val result = await(connector.getPayments(chargeReference))

      result shouldBe Right(OpsApiError(OK, "Invalid Json"))

      verify(mockHttpClient, times(1))
        .GET(
          any(),
          any(),
          any()
        )(
          any(),
          any(),
          any()
        )

      reset(mockHttpClient)
    }
  }
}
