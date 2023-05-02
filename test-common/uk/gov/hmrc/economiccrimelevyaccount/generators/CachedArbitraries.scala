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

import org.scalacheck.Arbitrary
import org.scalacheck.derive.MkArbitrary
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments}
import uk.gov.hmrc.economiccrimelevyaccount.EclTestData
import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import uk.gov.hmrc.economiccrimelevyaccount.models.ObligationData
import uk.gov.hmrc.economiccrimelevyaccount.models.eacd.QueryKnownFactsResponse

object CachedArbitraries extends EclTestData {

  private def mkArb[T](implicit mkArb: MkArbitrary[T]): Arbitrary[T] = MkArbitrary[T].arbitrary

  implicit lazy val arbEnrolment: Arbitrary[Enrolment]                             = mkArb
  implicit lazy val arbEnrolments: Arbitrary[Enrolments]                           = mkArb
  implicit lazy val arbQueryKnownFactsResponse: Arbitrary[QueryKnownFactsResponse] = mkArb
  implicit lazy val arbObligationData: Arbitrary[ObligationData]                   = mkArb

}
