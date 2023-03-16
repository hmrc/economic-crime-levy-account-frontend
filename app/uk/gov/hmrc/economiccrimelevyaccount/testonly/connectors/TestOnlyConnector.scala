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

package uk.gov.hmrc.economiccrimelevyaccount.testonly.connectors

import uk.gov.hmrc.economiccrimelevyaccount.config.AppConfig
import uk.gov.hmrc.http.HttpClient

import javax.inject.Inject
import scala.concurrent.ExecutionContext

class TestOnlyConnector @Inject() (
  appConfig: AppConfig,
  httpClient: HttpClient
)(implicit
  val ec: ExecutionContext
) {

  //TODO Implement test endpoint connector calls here

}
