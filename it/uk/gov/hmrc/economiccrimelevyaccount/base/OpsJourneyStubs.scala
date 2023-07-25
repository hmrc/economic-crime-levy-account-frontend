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

  def stubGetPayments(chargeReference: String): StubMapping =
    stub(
      get(urlEqualTo(s"/pay-api/v2/payment/search/$chargeReference")),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(PaymentBlock(
          searchTag = chargeReference,
          warnings = Seq.empty,
          payments = Seq(Payment(
            id = "testPaymentId",
            taxType = "testTaxType",
            status = "Successful",
            amountInPence = BigDecimal(1000),
            reference = "testReference",
            transactionReference = "testTrxReference",
            createdOn = LocalDateTime.now(),
          ))
        )).toString())
    )
}
