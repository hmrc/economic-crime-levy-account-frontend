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

package uk.gov.hmrc.economiccrimelevyaccount.views

import play.api.i18n.Messages

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object ViewUtils {

  def title(pageTitle: String, section: Option[String] = None)(implicit messages: Messages): String =
    s"${messages(pageTitle)} - ${section.fold("")(messages(_) + " - ")}${messages("service.name")} - ${messages("site.govuk")}"

  def formatLocalDate(localDate: LocalDate, translate: Boolean = true)(implicit messages: Messages): String =
    if (translate) {
      val day   = localDate.getDayOfMonth
      val month = messages(s"date.month.${localDate.getMonthValue}")
      val year  = localDate.getYear

      s"$day $month $year"
    } else {
      val formatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
      localDate.format(formatter)
    }

  def formatMoney(amount: BigDecimal): String =
    if (amount.isWhole) f"£$amount%,.0f" else f"£$amount%,.2f"

}
