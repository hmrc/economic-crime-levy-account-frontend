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

package uk.gov.hmrc.economiccrimelevyaccount.services

import cats.data.EitherT
import uk.gov.hmrc.economiccrimelevyaccount.config.AppConfig
import uk.gov.hmrc.economiccrimelevyaccount.connectors.OpsConnector
import uk.gov.hmrc.economiccrimelevyaccount.models.errors.OpsError
import uk.gov.hmrc.economiccrimelevyaccount.models.{OpsJourneyRequest, OpsJourneyResponse}
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class OpsService @Inject() (
  opsConnector: OpsConnector,
  appConfig: AppConfig
)(implicit ec: ExecutionContext) {

  private val dueMonth = 9
  private val dueDay   = 30

  def startOpsJourney(chargeReference: String, amount: BigDecimal, toDate: Option[LocalDate])(implicit
    hc: HeaderCarrier
  ): EitherT[Future, OpsError, OpsJourneyResponse] =
    EitherT {
      val dueDate = toDate.map(date => LocalDate.of(date.getYear, dueMonth, dueDay))

      val opsJourneyRequest = OpsJourneyRequest(
        chargeReference,
        amount * 100,
        appConfig.dashboardUrl,
        appConfig.dashboardUrl,
        dueDate
      )

      opsConnector
        .createOpsJourney(opsJourneyRequest)
        .map(Right(_))
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse
                .unapply(error)
                .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            Left(OpsError.BadGateway(reason = s"Start OPS Journey Failed - $message", code = code))
          case NonFatal(thr) => Left(OpsError.InternalUnexpectedError(thr.getMessage, Some(thr)))
        }
    }
}
