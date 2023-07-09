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
import uk.gov.hmrc.economiccrimelevyaccount.generators.CachedArbitraries.{genSameVale, localDateGen}
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
                                          "period-key"
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
                              "period-key"
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
                              "period-key"
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
      totalisation    <- Arbitrary.arbitrary[Totalisation]
      chargeReference <- Arbitrary.arbitrary[String]
      postingDateArb   =
        Arbitrary(localDateGen(currentYear - 1, startMonthFY, startDayFY, currentYear, endMonthFY, endDayFY))
      issueDateArb     =
        Arbitrary(localDateGen(currentYear - 1, startMonthFY, startDayFY, currentYear, endMonthFY, endDayFY))
      totalAmount      = Arbitrary(genSameVale(10_000))
      clearedAmount    = Arbitrary(genSameVale(1_000))
      documentDetails <- Arbitrary.arbitrary[DocumentDetails]
      lineItemDetails <- Arbitrary.arbitrary[LineItemDetails]
      itemFromDate     =
        Arbitrary(localDateGen(currentYear - 1, startMonthFY, startDayFY, currentYear, endMonthFY, endDayFY))
      itemToDate       = Arbitrary(localDateGen(currentYear, startMonthFY, startDayFY, currentYear, endMonthFY, endDayFY))
      itemNetDueDate   = Arbitrary(localDateGen(currentYear, startMonthFY, startDayFY, currentYear, endMonthFY, endDayFY))

    } yield ValidFinancialDataResponse(
      FinancialDataResponse(
        totalisation = Some(totalisation),
        documentDetails = Some(
          Seq(
            documentDetails.copy(
              documentType = Some(NewCharge),
              chargeReferenceNumber = Some(chargeReference),
              postingDate = Some(postingDateArb.toString),
              issueDate = Some(issueDateArb.toString),
              documentTotalAmount = Some(BigDecimal(totalAmount.toString)),
              documentClearedAmount = Some(BigDecimal(clearedAmount.toString)),
              documentOutstandingAmount = Some(BigDecimal(totalAmount.toString) - BigDecimal(clearedAmount.toString)),
              lineItemDetails = Some(
                Seq(
                  lineItemDetails.copy(
                    chargeDescription = Some(chargeReference),
                    periodFromDate = Some(itemFromDate.toString),
                    periodToDate = Some(itemToDate.toString),
                    periodKey = Some(calculatePeriodKey(postingDateArb.toString.takeRight(4))),
                    netDueDate = Some(itemNetDueDate.toString),
                    amount = Some(BigDecimal(clearedAmount.toString))
                  )
                )
              )
            )
          )
        )
      )
    )
  }

  def alphaNumericString: String = Gen.alphaNumStr.sample.get

  private def calculatePeriodKey(year: String): String = s"${year.takeRight(2)}XY"

  val testInternalId: String               = alphaNumericString
  val testEclRegistrationReference: String = alphaNumericString

}
