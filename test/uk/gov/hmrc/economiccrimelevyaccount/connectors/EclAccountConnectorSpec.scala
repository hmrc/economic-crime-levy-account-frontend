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
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyaccount.ObligationDataWithObligation
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.http.{HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import scala.concurrent.Future
import scala.util.{Failure, Try}

class EclAccountConnectorSpec extends SpecBase {

  val mockHttpClient: HttpClientV2       = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]

  val connector = new EclAccountConnector(
    appConfig,
    mockHttpClient,
    config,
    actorSystem
  )

  override def beforeEach(): Unit = {
    reset(mockRequestBuilder)
    reset(mockHttpClient)
  }

  "getObligationData" should {
    "return obligationData when the http client returns a successful http response" in forAll {
      (obligationDataWithObligation: ObligationDataWithObligation) =>
        when(mockHttpClient.get(ArgumentMatchers.eq(url"${appConfig.obligationDataUrl}"))(any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(
            Future.successful(
              HttpResponse.apply(OK, Json.stringify(Json.toJson(obligationDataWithObligation.obligationData)))
            )
          )

        val result = await(connector.getObligationData)

        result shouldBe Some(obligationDataWithObligation.obligationData)
    }

    "retries when a 500x error is returned from ECL account backend" in {
      beforeEach()

      val errorMessage = "internal server error"
      when(mockHttpClient.get(ArgumentMatchers.eq(url"${appConfig.obligationDataUrl}"))(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.setHeader(any()))
        .thenReturn(mockRequestBuilder)
      when(mockRequestBuilder.execute[HttpResponse](any(), any()))
        .thenReturn(Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, errorMessage)))

      Try(await(connector.getObligationData)) match {
        case Failure(UpstreamErrorResponse(msg, _, _, _)) =>
          msg shouldEqual errorMessage
        case _                                            => fail("expected UpstreamErrorResponse when an error is received from ECL account backend")
      }

      verify(mockRequestBuilder, times(1))
        .execute(any(), any())
    }
  }
}
