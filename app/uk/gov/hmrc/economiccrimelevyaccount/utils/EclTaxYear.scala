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

package uk.gov.hmrc.economiccrimelevyaccount.utils

import java.time.LocalDate
import uk.gov.hmrc.time.TaxYear

object EclTaxYear {

  private def startYear = TaxYear.taxYearFor(LocalDate.now()).startYear
  private val MonthDue  = 9
  private val DayDue    = 30

  def dueDate: LocalDate =
    LocalDate.of(startYear, MonthDue, DayDue)
}
