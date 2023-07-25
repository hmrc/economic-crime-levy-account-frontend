package uk.gov.hmrc.economiccrimelevyaccount.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.{CREATED, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyaccount.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyaccount.models.{ObligationData, OpsJourneyResponse, PaymentBlock}

import java.time.LocalDate

trait OpsJourneyStubs { self: WireMockStubs =>
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

  def stubGetPayments(paymentBlock: PaymentBlock): StubMapping =
    stub(
      get(urlEqualTo(s"/pay-api/v2/payment/search/${paymentBlock.searchTag}")),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(paymentBlock).toString())
    )
}
