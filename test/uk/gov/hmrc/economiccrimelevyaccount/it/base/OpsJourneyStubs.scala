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

import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import play.api.http.Status.CREATED
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyaccount.it.base.WireMockHelper._
import uk.gov.hmrc.economiccrimelevyaccount.models.OpsJourneyResponse

trait OpsJourneyStubs { self: WireMockStubs =>

  def stubStartJourney(url: String): StubMapping =
    stub(
      post(urlEqualTo("/pay-api/economic-crime-levy/journey/start")),
      aResponse()
        .withStatus(CREATED)
        .withBody(
          Json
            .toJson(
              OpsJourneyResponse(
                "",
                url
              )
            )
            .toString()
        )
    )

}
