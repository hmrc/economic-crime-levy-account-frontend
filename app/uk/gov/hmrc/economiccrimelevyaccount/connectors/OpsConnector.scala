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
import play.api.http.Status.{CREATED, OK}
import uk.gov.hmrc.economiccrimelevyaccount.config.AppConfig
import uk.gov.hmrc.economiccrimelevyaccount.models.{OpsJourneyRequest, OpsJourneyResponse, Payment, PaymentBlock}
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OpsConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClient
)(implicit ec: ExecutionContext) {

  private val opsServiceUrl: String  = s"${appConfig.opsStartJourneyUrl}"
  private val getPaymentsUrl: String = s"${appConfig.getPaymentsUrl}"

  def createOpsJourney(opsJourneyRequest: OpsJourneyRequest)(implicit
    hc: HeaderCarrier
  ): Future[Either[OpsApiError, OpsJourneyResponse]] =
    httpClient
      .POST[OpsJourneyRequest, HttpResponse](
        s"$opsServiceUrl",
        opsJourneyRequest,
        Seq(("x-session-id", opsJourneyRequest.chargeReference))
      )
      .map {
        case response if response.status == CREATED =>
          response.json
            .validate[OpsJourneyResponse]
            .fold(
              invalid => Left(OpsApiError(response.status, "Invalid Json")),
              valid => Right(valid)
            )
        case response                               =>
          Left(OpsApiError(response.status, response.body))
      }

  def getPayments(chargeReference: String)(implicit
    hc: HeaderCarrier
  ): Future[Either[OpsApiError, Seq[Payment]]] =
    httpClient
      .GET[HttpResponse](
        s"$getPaymentsUrl".replace("{chargeReference}", chargeReference)
      )
      .map {
        case response if response.status == OK =>
          response.json
            .validate[PaymentBlock]
            .fold(
              invalid => Left(OpsApiError(response.status, "Invalid Json")),
              valid => Right(valid.payments)
            )
        case response                          =>
          Left(OpsApiError(response.status, response.body))
      }
}

case class OpsApiError(
  status: Int,
  message: String
)
