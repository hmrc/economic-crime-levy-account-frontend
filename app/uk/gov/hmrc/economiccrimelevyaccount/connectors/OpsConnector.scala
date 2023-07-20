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

import com.google.inject.Singleton
import uk.gov.hmrc.economiccrimelevyaccount.config.AppConfig
import uk.gov.hmrc.economiccrimelevyaccount.models.{OpsJourneyRequest, OpsJourneyResponse}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OpsConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClient
)(implicit ec: ExecutionContext) {

  private val opsServiceUrl: String = s"${appConfig.opsServiceUrl}"

  def createOpsJourney(opsJourneyRequest: OpsJourneyRequest)(implicit
    hc: HeaderCarrier
  ): Future[Either[OpsJourneyResponse, OpsJourneyError]] =
    httpClient
      .POST[OpsJourneyRequest, HttpResponse](
        s"$opsServiceUrl",
        opsJourneyRequest,
        Seq(("x-session-id", opsJourneyRequest.chargeReference))
      )
      .map {
        case response if response.status == 201 =>
          response.json
            .validate[OpsJourneyResponse]
            .fold(
              invalid => Right(OpsJourneyError(response.status, "Invalid Json")),
              valid => Left(valid)
            )
        case response                           =>
          Right(OpsJourneyError(response.status, response.body))
      }
}

case class OpsJourneyError(
  status: Int,
  message: String
)
