/*
 * Copyright 2024 HM Revenue & Customs
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
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyaccount.models.EclSubscriptionStatus
import uk.gov.hmrc.http.{HttpResponse, StringContextOps}
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class EclRegistrationConnectorSpec extends SpecBase {

  val mockHttpClient: HttpClientV2       = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val connector                          = new EclRegistrationConnector(appConfig, mockHttpClient)

  "getSubscriptionStatus" should {
    "return an EclSubscriptionStatus when the http client returns an EclSubscriptionStatus" in forAll {
      (eclRegistrationReference: String, eclSubscriptionStatus: EclSubscriptionStatus) =>

        val expectedUrl = url"${appConfig.subscriptionStatusUrl}/ZECL/$eclRegistrationReference"
        val response    = HttpResponse(OK, Json.toJson(eclSubscriptionStatus).toString())

        when(mockHttpClient.get(ArgumentMatchers.eq(expectedUrl))(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any())).thenReturn(Future.successful(response))

        val result = await(connector.getSubscriptionStatus(eclRegistrationReference))

        result shouldBe eclSubscriptionStatus
    }

    "throw an UpstreamErrorResponse exception when the http client returns a error response" in forAll {
      eclRegistrationReference: String =>

        val expectedUrl = url"${appConfig.subscriptionStatusUrl}/ZECL/$eclRegistrationReference"
        val msg         = "Internal server error"

        when(mockHttpClient.get(ArgumentMatchers.eq(expectedUrl))(any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, msg)))

        Try(await(connector.getSubscriptionStatus(eclRegistrationReference))) match {
          case Failure(thr) => thr.getMessage shouldBe msg
          case Success(_)   => fail("expected exception to be thrown")
        }
    }
  }
}
