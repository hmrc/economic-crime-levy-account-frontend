package uk.gov.hmrc.economiccrimelevyaccount.base

import uk.gov.hmrc.economiccrimelevyaccount.base.WireMockHelper._
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyaccount.models.ObligationData

trait ObligationDataStubs { self: WireMockStubs =>

  def stubGetObligations(obligationData: ObligationData): StubMapping =
    stub(
      get(urlEqualTo(s"/economic-crime-levy-account/obligation-data")),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(obligationData).toString())
    )

}
