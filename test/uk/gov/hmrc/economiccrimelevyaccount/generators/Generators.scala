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

package uk.gov.hmrc.economiccrimelevyaccount.generators

import org.scalacheck.Gen

import java.time.LocalDate

trait Generators {

  def localDateGen(
    startYear: Int,
    startMonth: Int,
    startDay: Int,
    endYear: Int,
    endMonth: Int,
    endDay: Int
  ): Gen[LocalDate] = {
    val rangeStart = LocalDate.of(startYear, startMonth, startDay).toEpochDay
    val rangeEnd   = LocalDate.of(endYear, endMonth, endDay).toEpochDay
    Gen.choose(rangeStart, rangeEnd).map(i => LocalDate.ofEpochDay(i))
  }

  def intBetween(min: Int, max: Int): Gen[Int] =
    Gen.choose(min, max)

  def genSameVale[A](value: A): Gen[A] =
    Gen.const(value)
}
