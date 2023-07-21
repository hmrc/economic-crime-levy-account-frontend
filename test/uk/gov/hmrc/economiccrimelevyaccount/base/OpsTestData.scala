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

package uk.gov.hmrc.economiccrimelevyaccount.base

import uk.gov.hmrc.economiccrimelevyaccount.models.Payment
import uk.gov.hmrc.economiccrimelevyaccount.models.Payment.{FAILED, SUCCESSFUL}

import java.time.LocalDateTime

trait OpsTestData {
  def payments() = {
    val date = LocalDateTime.now()
    Seq(
      payment(100, SUCCESSFUL, date),
      payment( 50, SUCCESSFUL, date),
      payment(100, FAILED, date),
    )
  }

  private def payment(amount: BigDecimal, status: String, date: LocalDateTime) =
    Payment("", "", status, amount * 100, 0, "", "", date, None)
}
