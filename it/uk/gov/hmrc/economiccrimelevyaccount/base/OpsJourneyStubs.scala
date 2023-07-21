package uk.gov.hmrc.economiccrimelevyaccount.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.{CREATED, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyaccount.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyaccount.models.{ObligationData, OpsJourneyResponse}

import java.time.LocalDate

trait OpsJourneyStubs { self: WireMockStubs with OpsTestData =>

  def stubStartJourney(url: String): StubMapping =
    stub(
      post(urlEqualTo("/pay-api/economic-crime-levy/journey/start")),
      aResponse()
        .withStatus(CREATED)
        .withBody(Json.toJson(OpsJourneyResponse(
          "",
          url
        )).toString())
    )

  def stubGetPayments(chargeReference: String, date: LocalDate): StubMapping =
    stub(
      get(urlEqualTo(s"/pay-api/v2/payment/search/$chargeReference?searchScope=economic-crime-levy")),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(paymentBlock(chargeReference, date)).toString())
    )
}
