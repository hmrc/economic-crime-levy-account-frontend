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
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.models.{EclReference, KeyValue}
import uk.gov.hmrc.economiccrimelevyaccount.models.eacd.{EclEnrolment, EnrolmentResponse, QueryKnownFactsRequest}
import uk.gov.hmrc.http.{HttpResponse, StringContextOps, UpstreamErrorResponse}
import uk.gov.hmrc.economiccrimelevyaccount.generators.CachedArbitraries._
import uk.gov.hmrc.http.client.{HttpClientV2, RequestBuilder}

import scala.concurrent.Future
import scala.util.{Failure, Try}

class EnrolmentStoreProxyConnectorSpec extends SpecBase {

  val mockHttpClient: HttpClientV2       = mock[HttpClientV2]
  val mockRequestBuilder: RequestBuilder = mock[RequestBuilder]
  val connector                          = new EnrolmentStoreProxyConnectorImpl(appConfig, mockHttpClient, config, actorSystem)
  val enrolmentStoreUrl: String          = s"${appConfig.enrolmentStoreProxyBaseUrl}/enrolment-store-proxy/enrolment-store"

  override def beforeEach(): Unit = {
    reset(mockRequestBuilder)
    reset(mockHttpClient)
  }

  "queryKnownFacts" should {

    "return known facts when the http client returns known facts" in forAll {
      (eclRegistrationReference: EclReference, queryKnownFactsResponse: EnrolmentResponse) =>
        val queryKnownFactsRequest = QueryKnownFactsRequest(
          service = EclEnrolment.ServiceName,
          knownFacts = Seq(
            KeyValue(EclEnrolment.IdentifierKey, eclRegistrationReference.value)
          )
        )

        when(mockHttpClient.post(ArgumentMatchers.eq(url"${appConfig.enrolmentsUrl}"))(any()))
          .thenReturn(mockRequestBuilder)
        when(
          mockRequestBuilder
            .withBody(ArgumentMatchers.eq(Json.toJson(queryKnownFactsRequest)))(any(), any(), any())
        )
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(OK, Json.stringify(Json.toJson(queryKnownFactsResponse)))))

        val result = await(connector.getEnrolments(eclRegistrationReference))

        result shouldBe queryKnownFactsResponse
    }

    "retries when a 500x error is returned from Enrolment Store Proxy" in forAll {
      (eclRegistrationReference: EclReference) =>
        beforeEach()

        val queryKnownFactsRequest = QueryKnownFactsRequest(
          service = EclEnrolment.ServiceName,
          knownFacts = Seq(
            KeyValue(EclEnrolment.IdentifierKey, eclRegistrationReference.value)
          )
        )

        val errorMessage = "internal server error"
        when(mockHttpClient.post(ArgumentMatchers.eq(url"${appConfig.enrolmentsUrl}"))(any()))
          .thenReturn(mockRequestBuilder)
        when(
          mockRequestBuilder
            .withBody(ArgumentMatchers.eq(Json.toJson(queryKnownFactsRequest)))(any(), any(), any())
        )
          .thenReturn(mockRequestBuilder)
        when(mockRequestBuilder.execute[HttpResponse](any(), any()))
          .thenReturn(Future.successful(HttpResponse.apply(INTERNAL_SERVER_ERROR, errorMessage)))

        Try(await(connector.getEnrolments(eclRegistrationReference))) match {
          case Failure(UpstreamErrorResponse(msg, _, _, _)) =>
            msg shouldEqual errorMessage
          case _                                            => fail("expected UpstreamErrorResponse when an error is received from Enrolment Store Proxy")
        }

        verify(mockRequestBuilder, times(2))
          .execute(any(), any())
    }
  }
}
