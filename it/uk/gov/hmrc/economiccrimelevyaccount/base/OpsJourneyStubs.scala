package uk.gov.hmrc.economiccrimelevyaccount.base

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.{CREATED, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyaccount.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyaccount.models.{ObligationData, OpsJourneyResponse, Payment, PaymentBlock}

import java.time.{LocalDate, LocalDateTime}

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

  def stubGetPayments: StubMapping =
    stub(
      get(urlEqualTo(s"/pay-api/v2/payment/search/testReference")),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(Payment(
          paymentId = "testPaymentId",
          taxType = "testTaxType",
          status = "Successful",
          amountInPence = BigDecimal(1000),
          commissionInPence = BigDecimal(0),
          reference = "testReference",
          transactionReference = "testTrxReference",
          createdOn = LocalDateTime.now(),
          warning = None)).toString())
    )
}
