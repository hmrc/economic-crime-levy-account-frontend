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

import akka.actor.ActorSystem
import com.google.inject.Singleton
import com.typesafe.config.Config
import uk.gov.hmrc.economiccrimelevyaccount.config.AppConfig
import uk.gov.hmrc.economiccrimelevyaccount.models.{FinancialData, ObligationData}
import uk.gov.hmrc.http.{HeaderCarrier, Retries, StringContextOps}
import uk.gov.hmrc.http.client.HttpClientV2

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EclAccountConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2,
  override val configuration: Config,
  override val actorSystem: ActorSystem
)(implicit ec: ExecutionContext)
    extends BaseConnector
    with Retries {

  def getFinancialData(implicit
    hc: HeaderCarrier
  ): Future[Option[FinancialData]] =
    retryFor[Option[FinancialData]]("ECL Account - financial data")(retryCondition) {
      httpClient
        .get(url"${appConfig.financialDataUrl}")
        .executeAndDeserialiseOption[FinancialData]
    }

  def getObligationData(implicit hc: HeaderCarrier): Future[Option[ObligationData]] =
    retryFor[Option[ObligationData]]("ECL Account - obligation data")(retryCondition) {
      httpClient
        .get(url"${appConfig.obligationDataUrl}")
        .executeAndDeserialiseOption[ObligationData]
    }
}
