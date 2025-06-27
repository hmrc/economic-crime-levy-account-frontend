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

package uk.gov.hmrc.economiccrimelevyaccount

import com.danielasfregola.randomdatagenerator.RandomDataGenerator.derivedArbitrary
import org.scalacheck.{Arbitrary, Gen}
import uk.gov.hmrc.auth.core.{Enrolment, EnrolmentIdentifier, Enrolments}
import uk.gov.hmrc.economiccrimelevyaccount.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyaccount.models.EclSubscriptionStatus.{DeRegistered, Subscribed}
import uk.gov.hmrc.economiccrimelevyaccount.models.eacd.EclEnrolment
import uk.gov.hmrc.economiccrimelevyaccount.models._
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.PaymentStatus.Paid
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels._
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.PaymentType._
import uk.gov.hmrc.time.TaxYear

import java.time.{Instant, LocalDate}

case class EnrolmentsWithEcl(enrolments: Enrolments)

case class EnrolmentsWithoutEcl(enrolments: Enrolments)

case class ObligationDataWithObligation(obligationData: ObligationData)
case class ObligationDataWithOverdueObligation(obligationData: ObligationData)
case class ObligationDataWithSubmittedObligation(obligationData: ObligationData)

case class ValidFinancialDataResponse(financialDataResponse: FinancialData)
case class ValidFinancialDataResponseForLatestObligation(financialDataResponse: FinancialData)

case class ValidPaymentsViewModel(viewModel: PaymentsViewModel)

trait EclTestData {

  private val currentYear       = LocalDate.now().getYear
  private val startDayFY: Int   = 1
  private val endDayFY: Int     = 31
  private val startMonthFY: Int = 4
  private val endMonthFY: Int   = 3

  val uuidRegex: String = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$"

  def alphaNumericString: String = Gen.alphaNumStr.sample.get

  private val genDate: Gen[LocalDate] =
    localDateGen(currentYear - 1, startMonthFY, startDayFY, currentYear, endMonthFY, endDayFY)

  val testInternalId: String = alphaNumericString

  val testEclRegistrationReference: String = "test-ecl-registration-reference"

  val testEclReference: EclReference = EclReference(testEclRegistrationReference)

  val testDeregisteredSubscriptionStatus: EclSubscriptionStatus = EclSubscriptionStatus(
    DeRegistered(testEclRegistrationReference)
  )
  val testSubscribedSubscriptionStatus: EclSubscriptionStatus   = EclSubscriptionStatus(
    Subscribed(testEclRegistrationReference)
  )

  implicit val arbInstant: Arbitrary[Instant] = Arbitrary {
    Instant.now()
  }

  implicit val arbLocalDate: Arbitrary[LocalDate] = Arbitrary {
    LocalDate.now()
  }

  implicit val arbEnrolmentsWithEcl: Arbitrary[EnrolmentsWithEcl] = Arbitrary {
    for {
      enrolments               <- Arbitrary.arbitrary[Enrolments]
      enrolment                <- Arbitrary.arbitrary[Enrolment]
      eclRegistrationReference <- Arbitrary.arbitrary[String]
      eclEnrolmentIdentifier    = EnrolmentIdentifier(EclEnrolment.identifierKey, eclRegistrationReference)
      eclEnrolment              =
        enrolment.copy(key = EclEnrolment.serviceName, identifiers = enrolment.identifiers :+ eclEnrolmentIdentifier)
    } yield EnrolmentsWithEcl(enrolments.copy(enrolments.enrolments + eclEnrolment))
  }

  implicit val arbObligationDataWithObligation: Arbitrary[ObligationDataWithObligation] = Arbitrary {
    for {
      obligationData                 <- Arbitrary.arbitrary[ObligationData]
      obligation                     <- Arbitrary.arbitrary[Obligation]
      obligationDetails               = ObligationDetails(
                                          Open,
                                          LocalDate.now().minusYears(1),
                                          LocalDate.now(),
                                          Some(LocalDate.now()),
                                          LocalDate.now().plusDays(1),
                                          "21XY"
                                        )
      obligationWithObligationDetails = obligation.copy(Seq(obligationDetails))
    } yield ObligationDataWithObligation(obligationData.copy(Seq(obligationWithObligationDetails)))
  }

  implicit val arbObligationDataWithObligationThatIsOverdue: Arbitrary[ObligationDataWithOverdueObligation] =
    Arbitrary {
      for {
        obligationData   <- Arbitrary.arbitrary[ObligationData]
        obligation       <- Arbitrary.arbitrary[Obligation]
        obligationDetails = ObligationDetails(
                              Open,
                              LocalDate.now().minusYears(1),
                              LocalDate.now(),
                              Some(LocalDate.now()),
                              LocalDate.now().minusDays(1),
                              "21XY"
                            )
        overdueObligation = obligation.copy(Seq(obligationDetails))
      } yield ObligationDataWithOverdueObligation(obligationData.copy(Seq(overdueObligation)))
    }

  implicit val arbObligationDataWithObligationThatIsSubmitted: Arbitrary[ObligationDataWithSubmittedObligation] =
    Arbitrary {
      for {
        obligationData   <- Arbitrary.arbitrary[ObligationData]
        obligation       <- Arbitrary.arbitrary[Obligation]
        obligationDetails = ObligationDetails(
                              Fulfilled,
                              LocalDate.now().minusYears(1),
                              LocalDate.now(),
                              Some(LocalDate.now()),
                              LocalDate.now().plusDays(1),
                              "21XY"
                            )
        overdueObligation = obligation.copy(Seq(obligationDetails))
      } yield ObligationDataWithSubmittedObligation(obligationData.copy(Seq(overdueObligation)))
    }

  implicit val arbEnrolmentsWithoutEcl: Arbitrary[EnrolmentsWithoutEcl] = Arbitrary {
    Arbitrary
      .arbitrary[Enrolments]
      .retryUntil(
        !_.enrolments.exists(e =>
          e.key == EclEnrolment.serviceName && e.identifiers.exists(_.key == EclEnrolment.identifierKey)
        )
      )
      .map(EnrolmentsWithoutEcl)
  }

  implicit val arbValidFinancialDataResponse: Arbitrary[ValidFinancialDataResponse] = Arbitrary {
    for {
      penaltyTotals          <- Arbitrary.arbitrary[PenaltyTotals]
      randomString           <- Arbitrary.arbitrary[String]
      totalAmount            <- Arbitrary.arbitrary[Int]
      clearedAmount          <- Arbitrary.arbitrary[Int]
      arbTotalisationAmounts <- Arbitrary.arbitrary[Int]
      documentDetails        <- Arbitrary.arbitrary[DocumentDetails]
      lineItemDetails        <- Arbitrary.arbitrary[LineItemDetails]

    } yield ValidFinancialDataResponse(
      FinancialData(
        totalisation = Some(
          Totalisation(
            totalAccountBalance = Some(BigDecimal(arbTotalisationAmounts)),
            totalAccountOverdue = Some(BigDecimal(arbTotalisationAmounts.toString)),
            totalOverdue = Some(BigDecimal(arbTotalisationAmounts.toString)),
            totalNotYetDue = Some(BigDecimal(arbTotalisationAmounts.toString)),
            totalBalance = Some(BigDecimal(arbTotalisationAmounts.toString)),
            totalCredit = Some(BigDecimal(arbTotalisationAmounts.toString)),
            totalCleared = Some(BigDecimal(arbTotalisationAmounts.toString))
          )
        ),
        documentDetails = Some(
          Seq(
            documentDetails.copy(
              documentType = Some(NewCharge),
              chargeReferenceNumber = Some("test-ecl-registration-reference"),
              documentTotalAmount = Some(BigDecimal(totalAmount.toString)),
              documentClearedAmount = Some(BigDecimal(clearedAmount.toString)),
              documentOutstandingAmount = Some(BigDecimal(totalAmount.toString) - BigDecimal(clearedAmount.toString)),
              interestPostedAmount = Some(BigDecimal(arbTotalisationAmounts.toString)),
              interestAccruingAmount = Some(BigDecimal(arbTotalisationAmounts.toString)),
              issueDate = Some(LocalDate.now.toString),
              penaltyTotals = Some(
                Seq(
                  penaltyTotals.copy(
                    penaltyAmount = Some(BigDecimal(arbTotalisationAmounts.toString)),
                    penaltyStatus = Some(randomString)
                  )
                )
              ),
              lineItemDetails = Some(
                Seq(
                  lineItemDetails.copy(
                    chargeDescription = Some("test-ecl-registration-reference"),
                    amount = Some(BigDecimal(clearedAmount.toString)),
                    clearingDate = Some(LocalDate.now),
                    periodFromDate = Some(LocalDate.now),
                    periodToDate = Some(LocalDate.now),
                    periodKey = Some("21XY"),
                    mainTransaction = Some("6220"),
                    subTransaction = Some("3410")
                  )
                )
              )
            )
          )
        )
      )
    )
  }

  implicit val arbValidFinancialDataResponseForLatestObligation
    : Arbitrary[ValidFinancialDataResponseForLatestObligation] = Arbitrary {
    for {
      arbTotalisationAmounts <- Arbitrary.arbitrary[Int]
      documentDetails        <- Arbitrary.arbitrary[DocumentDetails]
      lineItemDetails        <- Arbitrary.arbitrary[LineItemDetails]

    } yield ValidFinancialDataResponseForLatestObligation(
      FinancialData(
        totalisation = Some(
          Totalisation(
            totalAccountBalance = Some(BigDecimal(arbTotalisationAmounts)),
            totalAccountOverdue = Some(BigDecimal(arbTotalisationAmounts.toString)),
            totalOverdue = Some(BigDecimal(arbTotalisationAmounts.toString)),
            totalNotYetDue = Some(BigDecimal(arbTotalisationAmounts.toString)),
            totalBalance = Some(BigDecimal(arbTotalisationAmounts.toString)),
            totalCredit = Some(BigDecimal(arbTotalisationAmounts.toString)),
            totalCleared = Some(BigDecimal(arbTotalisationAmounts.toString))
          )
        ),
        documentDetails = Some(
          Seq(
            documentDetails.copy(
              documentType = Some(NewCharge),
              chargeReferenceNumber = Some("test-ecl-registration-reference"),
              documentTotalAmount = Some(BigDecimal(10000)),
              documentClearedAmount = Some(BigDecimal(1000)),
              documentOutstandingAmount = Some(BigDecimal(9000)),
              interestPostedAmount = None,
              interestAccruingAmount = None,
              issueDate = Some(TaxYear.current.starts.toString),
              penaltyTotals = None,
              lineItemDetails = Some(
                Seq(
                  lineItemDetails.copy(
                    chargeDescription = Some("test-ecl-registration-reference"),
                    amount = Some(BigDecimal(1000)),
                    clearingDate = Some(LocalDate.now),
                    periodFromDate = Some(TaxYear.current.starts),
                    periodToDate = Some(TaxYear.current.starts),
                    periodKey = Some("21XY"),
                    clearingDocument = Some("clearing-document"),
                    clearingReason = Some("Incoming Payment"),
                    mainTransaction = Some("6220"),
                    subTransaction = Some("3410")
                  )
                )
              )
            )
          )
        )
      )
    )
  }

  implicit val arbFinancialDetails: Arbitrary[FinancialDetails] = Arbitrary {
    for {
      fromDate  <- genDate
      toDate    <- genDate
      periodKey <- Arbitrary.arbitrary[String]
      amount    <- Arbitrary.arbitrary[Int]

    } yield FinancialDetails(
      amount = BigDecimal(amount),
      fromDate = Some(fromDate),
      toDate = Some(toDate),
      periodKey = Some(periodKey),
      chargeReference = None,
      paymentType = StandardPayment
    )
  }

  implicit val arbPaymentsViewModel: Arbitrary[ValidPaymentsViewModel] = Arbitrary {
    for {
      fromDate <- genDate
      toDate   <- genDate
      amount   <- Arbitrary.arbitrary[Int]

    } yield ValidPaymentsViewModel(
      PaymentsViewModel(
        outstandingPayments = Seq(
          OutstandingPayments(
            paymentDueDate = fromDate,
            chargeReference = "test-ecl-reference",
            fyFrom = fromDate,
            fyTo = toDate,
            amount = amount,
            paymentStatus = Paid,
            paymentType = StandardPayment,
            interestChargeReference = Some("test-ecl-reference")
          )
        ),
        paymentHistory = Seq(
          PaymentHistory(
            paymentDate = fromDate,
            chargeReference = Some("test-ecl-reference"),
            fyFrom = Some(fromDate),
            fyTo = Some(toDate),
            amount = amount,
            paymentStatus = Paid,
            paymentDocument = "payment-document",
            paymentType = StandardPayment,
            refundAmount = BigDecimal(0)
          )
        ),
        testEclReference,
        testSubscribedSubscriptionStatus
      )
    )
  }

  implicit val arbEclReference: Arbitrary[EclReference] = Arbitrary {
    testEclReference
  }
}
