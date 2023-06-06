/*
 * Copyright 2023 HM Revenue & Customs
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

package uk.gov.hmrc.economiccrimelevyaccount.connectors

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.economiccrimelevyaccount.ObligationDataWithObligation
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.models.ObligationData
import uk.gov.hmrc.http.HttpClient

import scala.concurrent.Future

class ObligationDataConnectorSpec extends SpecBase {

  val mockHttpClient: HttpClient = mock[HttpClient]

  val connector = new ObligationDataConnector(
    appConfig,
    mockHttpClient
  )

  "getObligationData" should {
    "return obligationData when the http client returns a successful http response" in forAll {
      val eclAccountObligationUrl: String =
        s"${appConfig.economicCrimeLevyAccountBaseUrl}/economic-crime-levy-account/obligation-data"
      (obligationDataWithObligation: ObligationDataWithObligation) =>
        when(
          mockHttpClient.GET[Option[ObligationData]](
            ArgumentMatchers.eq(eclAccountObligationUrl),
            any(),
            any()
          )(any(), any(), any())
        ).thenReturn(Future.successful(Some(obligationDataWithObligation.obligationData)))

        val result = await(connector.getObligationData())

        result shouldBe Some(obligationDataWithObligation.obligationData)

        verify(mockHttpClient, times(1)).GET[Option[ObligationData]](
          ArgumentMatchers.eq(eclAccountObligationUrl),
          any(),
          any()
        )(any(), any(), any())

        reset(mockHttpClient)
    }
  }
}
