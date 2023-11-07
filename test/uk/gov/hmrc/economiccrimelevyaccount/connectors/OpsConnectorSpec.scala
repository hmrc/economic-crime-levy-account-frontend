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
import play.api.http.Status.{CREATED, INTERNAL_SERVER_ERROR}
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.models.{OpsJourneyRequest, OpsJourneyResponse}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}
import uk.gov.hmrc.http.{HttpClient, HttpResponse, StringContextOps, UpstreamErrorResponse}

import scala.concurrent.Future
import scala.util.{Failure, Try}

class OpsConnectorSpec extends SpecBase with BeforeAndAfterEach {

  val mockHttpClient: HttpClientV2       = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val connector                          = new OpsConnector(appConfig, mockHttpClient, config, actorSystem)
  val url                                = "http://google.co.uk"
  val expectedUrl: String                = "http://www.bbc.co.uk"

  override def beforeEach(): Unit =
    reset(mockHttpClient)

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

      when(mockHttpClient.post(ArgumentMatchers.eq(url"${appConfig.opsStartJourneyUrl}"))(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(ArgumentMatchers.eq(opsJourneyRequest))(any(), any(), any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(CREATED, Json.stringify(Json.toJson(opsJourneyResponse)))))

      val result = await(connector.createOpsJourney(opsJourneyRequest))

      result shouldBe Left(opsJourneyResponse)
    }

    "return an error if OPS journey creation fails" in forAll { (chargeReference: String, amount: BigDecimal) =>
      val opsJourneyRequest = OpsJourneyRequest(
        chargeReference,
        amount * 100,
        url,
        url,
        None
      )

      val errorMessage = "internal server error"
      when(mockHttpClient.get(any())(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, "internal server error")))

      Try(await(connector.createOpsJourney(opsJourneyRequest))) match {
        case Failure(UpstreamErrorResponse(msg, _, _, _)) =>
          msg shouldEqual errorMessage
        case _                                            => fail("expected UpstreamErrorResponse when an error is received from DES")
      }

    }
  }

  "return an error if invalid Json" in forAll { (chargeReference: String, amount: BigDecimal) =>
    val opsJourneyRequest = OpsJourneyRequest(
      chargeReference,
      amount * 100,
      url,
      url,
      None
    )

    when(mockHttpClient.get(any())(any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.setHeader(any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.withBody(any())(any(), any(), any())).thenReturn(mockRequestBuilder)
    when(mockRequestBuilder.execute[HttpResponse](any(), any()))
      .thenReturn(Future.successful(HttpResponse.apply(CREATED, "{}")))

    Try(await(connector.createOpsJourney(opsJourneyRequest))) match {
      case Failure(UpstreamErrorResponse(msg, _, _, _)) =>
        msg shouldEqual "invalid json"
      case _                                            => fail("expected UpstreamErrorResponse when an error is received from DES")
    }
  }
}
