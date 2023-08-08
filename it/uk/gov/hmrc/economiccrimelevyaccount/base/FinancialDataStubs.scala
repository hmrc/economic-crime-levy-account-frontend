package uk.gov.hmrc.economiccrimelevyaccount.base

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyaccount.base.WireMockHelper.stub
import uk.gov.hmrc.economiccrimelevyaccount.models._

trait FinancialDataStubs {
  self: WireMockStubs =>

  def stubFinancialData: StubMapping =
    stub(
      get(urlEqualTo("/economic-crime-levy-account/financial-data")),
      aResponse()
        .withStatus(OK)
        .withBody(Json.toJson(
          FinancialDataResponse(None, Some(Seq(DocumentDetails(
            documentType = Some(NewCharge),
            chargeReferenceNumber = Some("XMECL0000000006"),
            postingDate = Some("2022-03-31"),
            issueDate = Some("2022-03-31"),
            documentTotalAmount = Some(BigDecimal("10000")),
            documentClearedAmount = Some(BigDecimal("0")),
            documentOutstandingAmount = Some(BigDecimal("10000")),
            lineItemDetails = Some(Seq(LineItemDetails(
              chargeDescription = Some("XMECL0000000006"),
              periodFromDate = Some("2022-03-31"),
              periodToDate = Some("2023-04-01"),
              periodKey = Some("22XY"),
              netDueDate = Some("2023-09-30"),
              amount = Some(BigDecimal("0 ")),
              clearingDate = None,
              clearingDocument = None,
              clearingReason = None
            ),
            )),
            interestPostedAmount = Some(BigDecimal("10000")),
            interestAccruingAmount = Some(BigDecimal("10000")),
            interestPostedChargeRef = Some("XMECL0000000006"),
            penaltyTotals = None
          ))))
        ).toString())
    )

}
