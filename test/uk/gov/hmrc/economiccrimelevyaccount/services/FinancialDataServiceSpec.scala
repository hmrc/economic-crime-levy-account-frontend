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

import org.mockito.ArgumentMatchers.any
import uk.gov.hmrc.economiccrimelevyaccount.ValidFinancialDataResponseForLatestObligation
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.connectors.FinancialDataConnector
import uk.gov.hmrc.economiccrimelevyaccount.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyaccount.models.{FinancialDataResponse, FinancialDetails}
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.PaymentStatus.{Overdue, PartiallyPaid}
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.PaymentType.{Interest, StandardPayment}
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.{FinancialViewDetails, PaymentHistory}
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels._

import scala.concurrent.Future

class FinancialDataServiceSpec extends SpecBase {

  private val mockFinancialDataConnector = mock[FinancialDataConnector]

  val service = new FinancialDataService(mockFinancialDataConnector)

  "getLatestFinancialObligation" should {
    "return None if documentDetails are not present" in forAll { financialDataResponse: FinancialDataResponse =>
      val response = service.getLatestFinancialObligation(financialDataResponse.copy(documentDetails = None))

      response shouldBe None
    }

    "return Some value if documentDetails are present" in forAll {
      financialDataResponse: ValidFinancialDataResponseForLatestObligation =>
        val response        = service.getLatestFinancialObligation(financialDataResponse.financialDataResponse)
        val firstItem       = financialDataResponse.financialDataResponse.documentDetails.get.head.lineItemDetails.get.head
        val documentDetails = financialDataResponse.financialDataResponse.documentDetails.get.head
        response shouldBe Some(
          FinancialDetails(
            documentDetails.documentOutstandingAmount.get,
            firstItem.periodFromDate.get,
            firstItem.periodToDate.get,
            firstItem.periodKey.get,
            documentDetails.chargeReferenceNumber.get,
            documentDetails.getPaymentType
          )
        )
    }
  }
  "getFinancialDetails"          should {
    "return None if we receive None from financialDataConnector" in {

      when(mockFinancialDataConnector.getFinancialData()(any()))
        .thenReturn(Future.successful(None))

      val response = await(service.getFinancialDetails)

      response shouldBe None
    }

    "return Some with FinancialViewDetails if we receive correct response from financialDataConnector" in forAll {
      validResponse: ValidFinancialDataResponseForLatestObligation =>
        when(mockFinancialDataConnector.getFinancialData()(any()))
          .thenReturn(Future.successful(Some(validResponse.financialDataResponse)))

        val response        = await(service.getFinancialDetails)
        val documentDetails = validResponse.financialDataResponse.documentDetails.get.head
        val firstItem       = validResponse.financialDataResponse.documentDetails.get.head.lineItemDetails.get.head
        response shouldBe Some(
          FinancialViewDetails(
            Seq(
              OutstandingPayments(
                paymentDueDate = documentDetails.paymentDueDate.get,
                chargeReference = documentDetails.chargeReferenceNumber.get,
                fyFrom = firstItem.periodFromDate.get,
                fyTo = firstItem.periodToDate.get,
                amount = documentDetails.documentOutstandingAmount.get,
                paymentStatus = Overdue,
                paymentType = StandardPayment,
                interestChargeReference = None
              )
            ),
            Seq(
              PaymentHistory(
                paymentDate = firstItem.clearingDate.get,
                chargeReference = documentDetails.chargeReferenceNumber,
                fyFrom = firstItem.periodFromDate,
                fyTo = firstItem.periodToDate,
                amount = firstItem.amount.get,
                paymentStatus = PartiallyPaid,
                paymentDocument = firstItem.clearingDocument.get,
                paymentType = StandardPayment,
                refundAmount = BigDecimal(0)
              )
            )
          )
        )
    }

    "return empty payment history where there is a reversal item" in forAll {
      validResponse: ValidFinancialDataResponseForLatestObligation =>
        val firstItem       = validResponse.financialDataResponse.documentDetails.get.head.lineItemDetails.get.head.copy(
          clearingReason = Some("Reversal")
        )
        val documentDetails = validResponse.financialDataResponse.documentDetails.get.head

        when(mockFinancialDataConnector.getFinancialData()(any()))
          .thenReturn(
            Future.successful(
              Some(
                validResponse.financialDataResponse.copy(
                  documentDetails = Some(
                    Seq(
                      documentDetails.copy(lineItemDetails = Some(Seq(firstItem)))
                    )
                  )
                )
              )
            )
          )

        val response = await(service.getFinancialDetails)

        response shouldBe Some(
          FinancialViewDetails(
            outstandingPayments = Seq(
              OutstandingPayments(
                paymentDueDate = documentDetails.paymentDueDate.get,
                chargeReference = documentDetails.chargeReferenceNumber.get,
                fyFrom = firstItem.periodFromDate.get,
                fyTo = firstItem.periodToDate.get,
                amount = documentDetails.documentOutstandingAmount.get,
                paymentStatus = Overdue,
                paymentType = StandardPayment,
                interestChargeReference = None
              )
            ),
            paymentHistory = Seq.empty
          )
        )
    }

    "return Some with FinancialViewDetails with interest that is not yet formed into interest document" in forAll {
      validResponse: ValidFinancialDataResponseForLatestObligation =>
        val documentDetailsFirstItem = validResponse.financialDataResponse.documentDetails.get.head

        val updatedDocumentDetailsFirstItem = documentDetailsFirstItem.copy(
          interestAccruingAmount = Some(BigDecimal(15.00))
        )
        val validFinancialDataResponse      = validResponse.financialDataResponse.copy(documentDetails =
          Some(
            Seq(
              updatedDocumentDetailsFirstItem
            )
          )
        )

        when(mockFinancialDataConnector.getFinancialData()(any()))
          .thenReturn(Future.successful(Some(validFinancialDataResponse)))

        val response        = await(service.getFinancialDetails)
        val documentDetails = validResponse.financialDataResponse.documentDetails.get.head
        val firstItem       = validResponse.financialDataResponse.documentDetails.get.head.lineItemDetails.get.head
        response shouldBe Some(
          FinancialViewDetails(
            Seq(
              OutstandingPayments(
                paymentDueDate = documentDetails.paymentDueDate.get,
                chargeReference = documentDetails.chargeReferenceNumber.get,
                fyFrom = firstItem.periodFromDate.get,
                fyTo = firstItem.periodToDate.get,
                amount = documentDetails.documentOutstandingAmount.get,
                paymentStatus = Overdue,
                paymentType = StandardPayment,
                interestChargeReference = None
              ),
              OutstandingPayments(
                paymentDueDate = documentDetails.paymentDueDate.get,
                chargeReference = documentDetails.chargeReferenceNumber.get,
                fyFrom = firstItem.periodFromDate.get,
                fyTo = firstItem.periodToDate.get,
                amount = BigDecimal(15.00),
                paymentStatus = Overdue,
                paymentType = Interest,
                interestChargeReference = None
              )
            ),
            Seq(
              PaymentHistory(
                paymentDate = firstItem.clearingDate.get,
                chargeReference = documentDetails.chargeReferenceNumber,
                fyFrom = firstItem.periodFromDate,
                fyTo = firstItem.periodToDate,
                amount = firstItem.amount.get,
                paymentStatus = PartiallyPaid,
                paymentDocument = firstItem.clearingDocument.get,
                paymentType = StandardPayment,
                refundAmount = BigDecimal(0)
              )
            )
          )
        )
    }
  }
}
