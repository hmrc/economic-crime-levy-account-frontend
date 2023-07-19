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

import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import uk.gov.hmrc.economiccrimelevyaccount.connectors.{FinancialDataConnector, OpsConnector}
import uk.gov.hmrc.economiccrimelevyaccount.models.{FinancialDataErrorResponse, FinancialDataResponse, FinancialDetails, OpsJourneyRequest}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import uk.gov.hmrc.economiccrimelevyaccount.controllers.routes
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate

class OpsService @Inject() (
  opsConnector: OpsConnector
)(implicit hc: HeaderCarrier, ec: ExecutionContext) {
  def get(chargeReference: String, amount: BigDecimal, dueDate: Option[LocalDate] = None): Future[Result] =
    opsConnector
      .crezteOpsJourney(
        OpsJourneyRequest(
          chargeReference,
          amount * 100,
          routes.AccountController.onPageLoad().url,
          routes.AccountController.onPageLoad().url,
          dueDate
        )
      )
      .map(r => Redirect(r.nextUrl))
}
