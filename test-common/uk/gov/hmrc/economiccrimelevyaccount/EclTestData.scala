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
import uk.gov.hmrc.economiccrimelevyaccount.models.eacd.EclEnrolment
import uk.gov.hmrc.economiccrimelevyaccount.models._

import java.time.{Instant, LocalDate}

case class EnrolmentsWithEcl(enrolments: Enrolments)

case class EnrolmentsWithoutEcl(enrolments: Enrolments)

case class ObligationDataWithObligation(obligationData: ObligationData)
case class ObligationDataWithOverdueObligation(obligationData: ObligationData)
case class ObligationDataWithSubmittedObligation(obligationData: ObligationData)

case class ValidFinancialDataResponse(financialDataResponse: FinancialDataResponse)

trait EclTestData {

  private val currentYear       = LocalDate.now().getYear
  private val startDayFY: Int   = 1
  private val endDayFY: Int     = 31
  private val startMonthFY: Int = 4
  private val endMonthFY: Int   = 3

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
      eclEnrolmentIdentifier    = EnrolmentIdentifier(EclEnrolment.IdentifierKey, eclRegistrationReference)
      eclEnrolment              =
        enrolment.copy(key = EclEnrolment.ServiceName, identifiers = enrolment.identifiers :+ eclEnrolmentIdentifier)
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
          e.key == EclEnrolment.ServiceName && e.identifiers.exists(_.key == EclEnrolment.IdentifierKey)
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
      FinancialDataResponse(
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
                    periodKey = Some("21XY")
                  )
                )
              )
            )
          )
        )
      )
    )
  }
  private val genDate: Gen[LocalDate]                                               =
    localDateGen(currentYear - 1, startMonthFY, startDayFY, currentYear, endMonthFY, endDayFY)

  implicit val arbFinancialDetails: Arbitrary[FinancialDetails] = Arbitrary {
    for {
      fromDate  <- genDate
      toDate    <- genDate
      periodKey <- Arbitrary.arbitrary[String]
      amount    <- Arbitrary.arbitrary[Int]

    } yield FinancialDetails(
      amount = BigDecimal(amount),
      fromDate = fromDate,
      toDate = toDate,
      periodKey = periodKey,
      chargeReference = ""
    )
  }
  def alphaNumericString: String                                = Gen.alphaNumStr.sample.get

  val testInternalId: String               = alphaNumericString
  val testEclRegistrationReference: String = alphaNumericString

}
