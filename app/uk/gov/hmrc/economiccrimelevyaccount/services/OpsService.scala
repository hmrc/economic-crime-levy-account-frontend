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

import uk.gov.hmrc.economiccrimelevyaccount.config.AppConfig
import uk.gov.hmrc.economiccrimelevyaccount.connectors.{OpsApiError, OpsConnector}
import uk.gov.hmrc.economiccrimelevyaccount.controllers.routes
import uk.gov.hmrc.economiccrimelevyaccount.models.Payment.SUCCESSFUL
import uk.gov.hmrc.economiccrimelevyaccount.models.{OpsJourneyRequest, OpsJourneyResponse, Payment}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class OpsService @Inject() (
  opsConnector: OpsConnector,
  appConfig: AppConfig
)(implicit ec: ExecutionContext) {

  def startOpsJourney(chargeReference: String, amount: BigDecimal, dueDate: Option[LocalDate] = None)(implicit
    hc: HeaderCarrier
  ): Future[Either[OpsApiError, OpsJourneyResponse]] = {
    val url = appConfig.host + routes.AccountController.onPageLoad().url
    opsConnector
      .createOpsJourney(
        OpsJourneyRequest(
          chargeReference,
          amount * 100,
          url,
          url,
          dueDate
        )
      )
  }

  def getTotalPaid(chargeReference: String)(implicit
    hc: HeaderCarrier
  ): Future[BigDecimal] =
    opsConnector.getPayments(chargeReference).map {
      case Right(payments) =>
        payments
          .filter(_.status == SUCCESSFUL)
          .map(_.amountInPence / 100)
          .sum
      case Left(_)         => 0
    }
}
