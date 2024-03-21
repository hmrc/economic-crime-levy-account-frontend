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
import play.api.http.Status.BAD_GATEWAY
import uk.gov.hmrc.economiccrimelevyaccount.{ObligationDataWithObligation, ValidFinancialDataResponse}
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.models.EclReference
import org.mockito.ArgumentMatchers
import uk.gov.hmrc.economiccrimelevyaccount.models.errors.EclAccountError
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.economiccrimelevyaccount.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyaccount.models.audit.{AccountViewedAuditEvent, AccountViewedAuditFinancialDetails}

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
            AccountViewedAuditEvent(
              internalId = internalId,
              eclReference = eclReference.value,
              obligationDetails = obligationDataWithObligation.map(_.obligations.flatMap(_.obligationDetails)).get,
              financialDetails = financialDataResponse.map(AccountViewedAuditFinancialDetails.apply)
            ).extendedDataEvent
          )(any(), any())
        )
          .thenReturn(Future.failed(UpstreamErrorResponse(errMessage, errorCode)))

        val result = service.auditAccountViewed(
          ArgumentMatchers.eq(internalId),
          ArgumentMatchers.eq(eclReference),
          ArgumentMatchers.eq(Some(obligationData.obligationData)),
          ArgumentMatchers.eq(Some(financialData.financialDataResponse))
        )(any())

        result shouldBe Left(EclAccountError.BadGateway(errMessage, errorCode))

    }
  }

}
