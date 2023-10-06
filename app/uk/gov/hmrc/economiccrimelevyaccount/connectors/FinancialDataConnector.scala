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
import play.api.http.Status.{NOT_FOUND, OK}
import uk.gov.hmrc.economiccrimelevyaccount.config.AppConfig
import uk.gov.hmrc.economiccrimelevyaccount.models.FinancialDataResponse
import uk.gov.hmrc.http.{HeaderCarrier, HttpClient, HttpResponse}
import uk.gov.hmrc.http.HttpReads.Implicits._
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FinancialDataConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClient
)(implicit ec: ExecutionContext)
    extends BaseConnector {

  private val eclAccountUrl: String = s"${appConfig.economicCrimeLevyAccountBaseUrl}/economic-crime-levy-account"

  def getFinancialData()(implicit
    hc: HeaderCarrier
  ): Future[Option[FinancialDataResponse]] =
    httpClient
      .GET[HttpResponse](
        s"$eclAccountUrl/financial-data"
      )
      .flatMap { response =>
        response.status match {
          case OK        => response.as[FinancialDataResponse].map(Some(_))
          case NOT_FOUND => Future.successful(None)
          case _         =>
            response.error
        }
      }
}
