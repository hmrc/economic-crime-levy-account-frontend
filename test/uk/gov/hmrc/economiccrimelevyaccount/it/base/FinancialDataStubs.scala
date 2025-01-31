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

import com.github.tomakehurst.wiremock.client.WireMock.{aResponse, get, urlEqualTo}
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyaccount.it.base.WireMockHelper.stub
import uk.gov.hmrc.economiccrimelevyaccount.models._

import java.time.LocalDate

trait FinancialDataStubs {
  self: WireMockStubs =>

  val periodKey = "22XY"

  def stubFinancialData(): StubMapping =
    stub(
      get(urlEqualTo("/economic-crime-levy-account/financial-data")),
      aResponse()
        .withStatus(OK)
        .withBody(
          Json
            .toJson(
              FinancialData(
                None,
                Some(
                  Seq(
                    DocumentDetails(
                      documentType = Some(NewCharge),
                      chargeReferenceNumber = Some("XMECL0000000006"),
                      postingDate = Some("2022-03-31"),
                      issueDate = Some("2022-03-31"),
                      documentTotalAmount = Some(BigDecimal("10000")),
                      documentClearedAmount = Some(BigDecimal("0")),
                      documentOutstandingAmount = Some(BigDecimal("10000")),
                      lineItemDetails = Some(
                        Seq(
                          LineItemDetails(
                            chargeDescription = Some("XMECL0000000006"),
                            periodFromDate = Some(LocalDate.parse("2022-03-31")),
                            periodToDate = Some(LocalDate.parse("2023-04-01")),
                            periodKey = Some(periodKey),
                            netDueDate = Some(LocalDate.parse("2023-09-30")),
                            amount = Some(BigDecimal("0")),
                            clearingDate = None,
                            clearingDocument = None,
                            clearingReason = Some("Incoming Payment")
                          )
                        )
                      ),
                      interestPostedAmount = Some(BigDecimal("10000")),
                      interestAccruingAmount = Some(BigDecimal("10000")),
                      interestPostedChargeRef = Some("XMECL0000000006"),
                      penaltyTotals = None,
                      contractObjectNumber = Some("00000290000000000173"),
                      contractObjectType = Some("ECL")
                    )
                  )
                )
              )
            )
            .toString()
        )
    )

  def stubFinancialDataError(): StubMapping =
    stub(
      get(urlEqualTo("/economic-crime-levy-account/financial-data")),
      aResponse()
        .withStatus(INTERNAL_SERVER_ERROR)
        .withBody("Internal server error")
    )
}
