package uk.gov.hmrc.economiccrimelevyaccount.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.{OK, UNAUTHORIZED}
import uk.gov.hmrc.economiccrimelevyaccount.base.WireMockHelper._
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
               |    "enrolment": "${EclEnrolment.ServiceName}",
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
             |    "key": "${EclEnrolment.ServiceName}",
             |    "identifiers": [{ "key":"${EclEnrolment.IdentifierKey}", "value": "$testEclRegistrationReference" }],
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
               |    "enrolment": "${EclEnrolment.ServiceName}",
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
               |    "enrolment": "${EclEnrolment.ServiceName}",
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
