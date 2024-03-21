/*
 * Copyright 2024 HM Revenue & Customs
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

import org.mockito.ArgumentMatchers.any
import play.api.http.Status.{BAD_GATEWAY, BAD_REQUEST}
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.models.EclReference
import uk.gov.hmrc.economiccrimelevyaccount.models.errors.AuditError
import uk.gov.hmrc.economiccrimelevyaccount.{ObligationDataWithObligation, ValidFinancialDataResponse}
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector

import scala.concurrent.Future

class AuditServiceSpec extends SpecBase {

  private val mockAuditConnector: AuditConnector = mock[AuditConnector]
  private val service: AuditService              = new AuditService(mockAuditConnector)

  "auditAccountViewed" should {
    "return AuditError.BadGateway when call to AuditConnector fails with 5xx error" in forAll {
      (
        obligationData: ObligationDataWithObligation,
        financialData: ValidFinancialDataResponse
      ) =>
        val errorCode  = BAD_GATEWAY
        val errMessage = "ErrorMessage"

        val eclReference                 = EclReference("eclreference")
        val obligationDataWithObligation = Some(obligationData.obligationData)
        val financialDataResponse        = Some(financialData.financialDataResponse)
        val internalId                   = "internalId"

        when(
          mockAuditConnector.sendExtendedEvent(
            any()
          )(any(), any())
        )
          .thenReturn(Future.failed(UpstreamErrorResponse(errMessage, errorCode)))

        val result = await(
          service
            .auditAccountViewed(
              internalId,
              eclReference,
              obligationDataWithObligation,
              financialDataResponse
            )
            .value
        )

        result shouldBe Left(AuditError.BadGateway(errMessage, errorCode))

    }

    "return AuditError.BadGateway when call to AuditConnector fails with 4xx error" in forAll {
      (
        obligationData: ObligationDataWithObligation,
        financialData: ValidFinancialDataResponse
      ) =>
        val errorCode  = BAD_REQUEST
        val errMessage = "ErrorMessage"

        val eclReference                 = EclReference("eclreference")
        val obligationDataWithObligation = Some(obligationData.obligationData)
        val financialDataResponse        = Some(financialData.financialDataResponse)
        val internalId                   = "internalId"

        when(
          mockAuditConnector.sendExtendedEvent(
            any()
          )(any(), any())
        )
          .thenReturn(Future.failed(UpstreamErrorResponse(errMessage, errorCode)))

        val result = await(
          service
            .auditAccountViewed(
              internalId,
              eclReference,
              obligationDataWithObligation,
              financialDataResponse
            )
            .value
        )

        result shouldBe Left(AuditError.BadGateway(errMessage, errorCode))

    }

    "return AuditError.InternalUnexpectedError when call to AuditConnector fails with NonFatal error" in forAll {
      (
        obligationData: ObligationDataWithObligation,
        financialData: ValidFinancialDataResponse
      ) =>
        val errMessage = "ErrorMessage"
        val exception  = new Exception(errMessage)

        val eclReference                 = EclReference("eclreference")
        val obligationDataWithObligation = Some(obligationData.obligationData)
        val financialDataResponse        = Some(financialData.financialDataResponse)
        val internalId                   = "internalId"

        when(
          mockAuditConnector.sendExtendedEvent(
            any()
          )(any(), any())
        )
          .thenReturn(Future.failed(exception))

        val result = await(
          service
            .auditAccountViewed(
              internalId,
              eclReference,
              obligationDataWithObligation,
              financialDataResponse
            )
            .value
        )

        result shouldBe Left(AuditError.InternalUnexpectedError(errMessage, Some(exception)))

    }
  }

}
