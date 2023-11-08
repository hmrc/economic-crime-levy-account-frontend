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

package uk.gov.hmrc.economiccrimelevyaccount.services

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.economiccrimelevyaccount.models.{EclReference, KeyValue}
import uk.gov.hmrc.economiccrimelevyaccount.models.eacd.{EclEnrolment, Enrolment, EnrolmentResponse}
import uk.gov.hmrc.economiccrimelevyaccount.models.errors.EnrolmentStoreError

import java.time.LocalDate
import scala.concurrent.Future
import scala.util.{Failure, Try}

class EnrolmentStoreProxyServiceSpec extends SpecBase {
  val mockEnrolmentStoreProxyConnector: EnrolmentStoreProxyConnector = mock[EnrolmentStoreProxyConnector]
  val service                                                        = new EnrolmentStoreProxyService(mockEnrolmentStoreProxyConnector)

  "getEclRegistrationDate" should {
    "return the ECL registration date from the query known facts response" in forAll {
      eclRegistrationReference: EclReference =>
        val queryKnownFactsResponse = EnrolmentResponse(
          service = EclEnrolment.ServiceName,
          enrolments = Seq(
            Enrolment(
              identifiers = Seq(KeyValue(key = EclEnrolment.IdentifierKey, value = eclRegistrationReference.value)),
              verifiers = Seq(KeyValue(key = EclEnrolment.RegistrationDateKey, value = "20230131"))
            )
          )
        )

        when(mockEnrolmentStoreProxyConnector.getEnrolments(any[String].asInstanceOf[EclReference])(any()))
          .thenReturn(Future.successful(queryKnownFactsResponse))

        val result = await(service.getEclRegistrationDate(eclRegistrationReference).value)

        result shouldBe Right(LocalDate.parse("2023-01-31"))
    }

    "return Left of EnrolmentStoreError if the ECL registration date could not be found in the enrolment" in forAll {
      eclRegistrationReference: EclReference =>
        val queryKnownFactsResponse = EnrolmentResponse(
          service = EclEnrolment.ServiceName,
          enrolments = Seq.empty
        )

        when(mockEnrolmentStoreProxyConnector.getEnrolments(any[String].asInstanceOf[EclReference])(any()))
          .thenReturn(Future.successful(queryKnownFactsResponse))

        val result = await(service.getEclRegistrationDate(eclRegistrationReference).value)

        result shouldBe Left(EnrolmentStoreError.InternalUnexpectedError("Missing registrationDate", None))
    }
  }

}
