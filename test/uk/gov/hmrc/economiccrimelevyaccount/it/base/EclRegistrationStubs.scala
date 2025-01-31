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
import play.api.http.Status.OK
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyaccount.it.base.WireMockHelper.stub
import uk.gov.hmrc.economiccrimelevyaccount.models.{EclReference, EclSubscriptionStatus}

trait EclRegistrationStubs { self: WireMockStubs =>

  def stubGetSubscriptionStatus(eclReference: EclReference, eclSubscriptionStatus: EclSubscriptionStatus): StubMapping =
    stub(
      get(urlEqualTo(s"/economic-crime-levy-registration/subscription-status/ZECL/${eclReference.value}")),
      aResponse()
        .withStatus(OK)
        .withBody(
          Json
            .toJson(
              eclSubscriptionStatus
            )
            .toString()
        )
    )
}
