package uk.gov.hmrc.economiccrimelevyaccount.base

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyaccount.base.WireMockHelper.stub
import uk.gov.hmrc.economiccrimelevyaccount.models.{EclReference, EclSubscriptionStatus}

trait EclRegistrationStubs { self: WireMockStubs =>

  def stubGetSubscriptionStatus(eclReference: EclReference, eclSubscriptionStatus: EclSubscriptionStatus): StubMapping =
    stub(
      get(urlEqualTo(s"/economic-crime-levy-registration/subscription-status/ZECL/${eclReference.value}")),
      aResponse()
        .withStatus(OK)
        .withBody(
          Json.toJson(
            eclSubscriptionStatus
          ).toString()
        )
    )
}
