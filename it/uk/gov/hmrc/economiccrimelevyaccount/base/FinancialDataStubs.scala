package uk.gov.hmrc.economiccrimelevyaccount.base

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyaccount.base.WireMockHelper.stub
import uk.gov.hmrc.economiccrimelevyaccount.models._

trait FinancialDataStubs { self: WireMockStubs =>

  def stubFinancialData: StubMapping =
    stub(
      get(urlEqualTo("/economic-crime-levy-account/financial-data")),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(
          FinancialDataResponse(None, Some(Seq(DocumentDetails(
            Some(NewCharge),
            Some("XMECL0000000006"),
            Some("2022-03-31"),
            Some("2022-03-31"),
            Some(BigDecimal("10000")),
            Some(BigDecimal("0")),
            Some(BigDecimal("10000")),
            Some(Seq(LineItemDetails(
              Some("XMECL0000000006"),
              Some("2022-03-31"),
              Some("2023-04-01"),
              Some("22XY"),
              Some("2023-09-30"),
              Some(BigDecimal("0")),
              None
            )
            ))
          ))))
        ).toString())
    )

}
