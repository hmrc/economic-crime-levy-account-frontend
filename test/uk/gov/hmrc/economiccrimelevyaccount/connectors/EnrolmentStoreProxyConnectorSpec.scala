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
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.models.KeyValue
import uk.gov.hmrc.economiccrimelevyaccount.models.eacd.{EclEnrolment, QueryKnownFactsRequest, EnrolmentResponse}
import uk.gov.hmrc.http.{HttpResponse, StringContextOps}
import uk.gov.hmrc.economiccrimelevyaccount.generators.CachedArbitraries._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import scala.concurrent.Future

class EnrolmentStoreProxyConnectorSpec extends SpecBase {

  val mockHttpClient: HttpClientV2       = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val connector                          = new EnrolmentStoreProxyConnectorImpl(appConfig, mockHttpClient)
  val enrolmentStoreUrl: String          = s"${appConfig.enrolmentStoreProxyBaseUrl}/enrolment-store-proxy/enrolment-store"

  override def beforeEach(): Unit = {
    reset(mockRequestBuilder)
    reset(mockHttpClient)
  }

  "queryKnownFacts" should {

    "return known facts when the http client returns known facts" in forAll {
      (eclRegistrationReference: String, queryKnownFactsResponse: EnrolmentResponse) =>
        val expectedUrl                    = s"$enrolmentStoreUrl/enrolments"
        val expectedQueryKnownFactsRequest = QueryKnownFactsRequest(
          service = EclEnrolment.ServiceName,
          knownFacts = Seq(
            KeyValue(EclEnrolment.IdentifierKey, eclRegistrationReference)
          )
        )

        when(mockHttpClient.post(ArgumentMatchers.eq(url"$expectedUrl"))(any())).thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.setHeader(any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.withBody(ArgumentMatchers.eq(expectedQueryKnownFactsRequest))(any(), any(), any()))
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(OK, Json.stringify(Json.toJson(queryKnownFactsResponse)))))

        val result = await(connector.getEnrolments(eclRegistrationReference))

        result shouldBe queryKnownFactsResponse
    }
  }
}
