/*
 * Copyright 2025 HM Revenue & Customs
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

package uk.gov.hmrc.economiccrimelevyaccount.it.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.{OK, UNAUTHORIZED}
import uk.gov.hmrc.economiccrimelevyaccount.it.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyaccount.models.eacd.EclEnrolment

trait AuthStubs { self: WireMockStubs =>

  def stubAuthorised(): StubMapping =
    stub(
      post(urlEqualTo("/auth/authorise"))
        .withRequestBody(
          equalToJson(
            s"""
               |{
               |  "authorise": [ {
               |    "enrolment": "${EclEnrolment.serviceName}",
               |    "identifiers": [],
               |    "state": "Activated"
               |  } ],
               |  "retrieve": [ "internalId", "authorisedEnrolments" ]
               |}
           """.stripMargin,
            true,
            true
          )
        ),
      aResponse()
        .withStatus(OK)
        .withBody(s"""
             |{
             |  "internalId": "$testInternalId",
             |  "authorisedEnrolments": [ {
             |    "key": "${EclEnrolment.serviceName}",
             |    "identifiers": [{ "key":"${EclEnrolment.identifierKey}", "value": "$testEclRegistrationReference" }],
             |    "state": "activated"
             |  } ]
             |}
         """.stripMargin)
    )

  def stubInsufficientEnrolments(): StubMapping =
    stub(
      post(urlEqualTo("/auth/authorise"))
        .withRequestBody(
          equalToJson(
            s"""
               |{
               |  "authorise": [ {
               |    "enrolment": "${EclEnrolment.serviceName}",
               |    "identifiers": [],
               |    "state": "Activated"
               |  } ],
               |  "retrieve": [ "internalId", "authorisedEnrolments" ]
               |}
           """.stripMargin,
            true,
            true
          )
        ),
      aResponse()
        .withStatus(UNAUTHORIZED)
        .withHeader("WWW-Authenticate", "MDTP detail=\"InsufficientEnrolments\"")
    )

  def stubUnsupportedAffinityGroup(): StubMapping =
    stub(
      post(urlEqualTo("/auth/authorise"))
        .withRequestBody(
          equalToJson(
            s"""
               |{
               |  "authorise": [ {
               |    "enrolment": "${EclEnrolment.serviceName}",
               |    "identifiers": [],
               |    "state": "Activated"
               |  } ],
               |  "retrieve": [ "internalId", "authorisedEnrolments" ]
               |}
           """.stripMargin,
            true,
            true
          )
        ),
      aResponse()
        .withStatus(UNAUTHORIZED)
        .withHeader("WWW-Authenticate", "MDTP detail=\"UnsupportedAffinityGroup\"")
    )

}
