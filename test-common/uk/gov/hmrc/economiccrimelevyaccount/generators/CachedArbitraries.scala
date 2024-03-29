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

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import org.scalacheck.Arbitrary
import org.scalacheck.derive.MkArbitrary
import uk.gov.hmrc.auth.core.{Enrolment, Enrolments}
import uk.gov.hmrc.economiccrimelevyaccount.EclTestData
import uk.gov.hmrc.economiccrimelevyaccount.models._
import uk.gov.hmrc.economiccrimelevyaccount.models.audit.AccountViewedAuditFinancialDetails
import uk.gov.hmrc.economiccrimelevyaccount.models.errors.ErrorCode
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.PaymentType

object CachedArbitraries extends EclTestData with Generators {

  private def mkArb[T](implicit mkArb: MkArbitrary[T]): Arbitrary[T] = MkArbitrary[T].arbitrary

  implicit lazy val arbEnrolment: Arbitrary[Enrolment]                                                   = mkArb
  implicit lazy val arbEnrolments: Arbitrary[Enrolments]                                                 = mkArb
  implicit lazy val arbObligationData: Arbitrary[ObligationData]                                         = mkArb
  implicit lazy val arbObligation: Arbitrary[Obligation]                                                 = mkArb
  implicit lazy val arbFinancialDataResponse: Arbitrary[FinancialData]                                   = mkArb
  implicit lazy val arbSubscriptionStatus: Arbitrary[SubscriptionStatus]                                 = mkArb
  implicit lazy val arbEclSubscriptionStatus: Arbitrary[EclSubscriptionStatus]                           = mkArb
  implicit lazy val arbPaymentType: Arbitrary[PaymentType]                                               = mkArb
  implicit lazy val arbPenaltyTotals: Arbitrary[PenaltyTotals]                                           = mkArb
  implicit lazy val arbErrorCodes: Arbitrary[ErrorCode]                                                  = mkArb
  implicit lazy val arbFinancialDataDocumentType: Arbitrary[FinancialDataDocumentType]                   = mkArb
  implicit lazy val arbObligationStatus: Arbitrary[ObligationStatus]                                     = mkArb
  implicit lazy val arbAccountViewedAuditFinancialDetails: Arbitrary[AccountViewedAuditFinancialDetails] = mkArb
  implicit lazy val arbDocumentDetails: Arbitrary[DocumentDetails]                                       = mkArb
  implicit lazy val arbLineItemDetails: Arbitrary[LineItemDetails]                                       = mkArb
}
