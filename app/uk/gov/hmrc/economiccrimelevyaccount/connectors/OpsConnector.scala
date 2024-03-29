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
import com.typesafe.config.Config
import org.apache.pekko.actor.ActorSystem
import play.api.libs.json.Json
import uk.gov.hmrc.economiccrimelevyaccount.config.AppConfig
import uk.gov.hmrc.economiccrimelevyaccount.models.{OpsJourneyRequest, OpsJourneyResponse}
import uk.gov.hmrc.economiccrimelevyaccount.utils.HttpHeader
import uk.gov.hmrc.http.client.HttpClientV2
import uk.gov.hmrc.http.{HeaderCarrier, Retries, StringContextOps}

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class OpsConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClientV2,
  override val configuration: Config,
  override val actorSystem: ActorSystem
)(implicit ec: ExecutionContext)
    extends BaseConnector
    with Retries {

  def createOpsJourney(opsJourneyRequest: OpsJourneyRequest)(implicit
    hc: HeaderCarrier
  ): Future[OpsJourneyResponse] =
    retryFor[OpsJourneyResponse]("OPS - Journey data")(retryCondition) {
      httpClient
        .post(url"${appConfig.opsStartJourneyUrl}")
        .withBody(Json.toJson(opsJourneyRequest))
        .transform(_.addHttpHeaders((HttpHeader.xSessionId, opsJourneyRequest.chargeReference)))
        .executeAndDeserialise[OpsJourneyResponse]
    }

}
