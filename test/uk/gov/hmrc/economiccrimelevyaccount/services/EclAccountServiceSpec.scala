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
import play.api.http.Status.BAD_REQUEST
import uk.gov.hmrc.economiccrimelevyaccount.ValidFinancialDataResponseForLatestObligation
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.connectors.EclAccountConnector
import uk.gov.hmrc.economiccrimelevyaccount.models.InterestCharge
import uk.gov.hmrc.economiccrimelevyaccount.models.errors.EclAccountError
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.PaymentStatus.{Overdue, PartiallyPaid}
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.PaymentType.{Interest, StandardPayment}
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels._
import uk.gov.hmrc.http.UpstreamErrorResponse
import uk.gov.hmrc.time.TaxYear

import java.time.LocalDate
import scala.concurrent.Future

class EclAccountServiceSpec extends SpecBase {

  private val mockECLAccountConnector = mock[EclAccountConnector]

  val service = new EclAccountService(mockECLAccountConnector)

  "getFinancialDetails" should {
    "return None if we receive None from financialDataConnector" in {

      when(mockECLAccountConnector.getFinancialData(any()))
        .thenReturn(Future.successful(None))

      val response = await(service.prepareViewModel(None, testEclReference, testSubscribedSubscriptionStatus).value)

      response shouldBe Right(None)
    }

    "return Some with PaymentsViewModel if we receive correct response from financialDataConnector" in forAll {
      validResponse: ValidFinancialDataResponseForLatestObligation =>
        when(mockECLAccountConnector.getFinancialData(any()))
          .thenReturn(Future.successful(Some(validResponse.financialDataResponse)))

        val response        = await(
          service
            .prepareViewModel(
              Some(validResponse.financialDataResponse),
              testEclReference,
              testSubscribedSubscriptionStatus
            )
            .value
        )
        val documentDetails = validResponse.financialDataResponse.documentDetails.get.head
        val firstItem       = validResponse.financialDataResponse.documentDetails.get.head.lineItemDetails.get.head
        response shouldBe Right(
          Some(
            PaymentsViewModel(
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
              ),
              testEclReference,
              testSubscribedSubscriptionStatus
            )
          )
        )
    }

    "return EclAccountError.InternalUnexpectedError when getFinancialData call fails" in forAll {
      validResponse: ValidFinancialDataResponseForLatestObligation =>
        val updatedDocumentDetails =
          validResponse.financialDataResponse.documentDetails.get(0) copy (documentType = Some(InterestCharge))

        val updatedResponse =
          validResponse.financialDataResponse.copy(documentDetails = Some(Seq(updatedDocumentDetails)))

        when(mockECLAccountConnector.getFinancialData(any()))
          .thenReturn(Future.successful(Some(updatedResponse)))

        val response = await(
          service
            .prepareViewModel(
              Some(updatedResponse),
              testEclReference,
              testSubscribedSubscriptionStatus
            )
            .value
        )
        response shouldBe Left(
          EclAccountError.InternalUnexpectedError("Missing data required for PaymentsViewModel", None)
        )
    }

    "return filled payment history where there is a reversal item" in forAll {
      validResponse: ValidFinancialDataResponseForLatestObligation =>
        val firstItem       = validResponse.financialDataResponse.documentDetails.get.head.lineItemDetails.get.head.copy(
          clearingReason = Some("Reversal")
        )
        val documentDetails = validResponse.financialDataResponse.documentDetails.get.head

        val financialDataResponse = Some(
          validResponse.financialDataResponse.copy(
            documentDetails = Some(
              Seq(
                documentDetails.copy(lineItemDetails = Some(Seq(firstItem)))
              )
            )
          )
        )

        when(mockECLAccountConnector.getFinancialData(any()))
          .thenReturn(
            Future.successful(
              financialDataResponse
            )
          )

        await(
          service.prepareViewModel(financialDataResponse, testEclReference, testSubscribedSubscriptionStatus).value
        ) shouldBe Right(
          Some(
            PaymentsViewModel(
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
              paymentHistory = Seq(
                PaymentHistory(
                  paymentDate = LocalDate.now,
                  chargeReference = Some("test-ecl-registration-reference"),
                  fyFrom = Some(TaxYear.current.starts),
                  fyTo = Some(TaxYear.current.starts),
                  amount = BigDecimal(1000),
                  paymentStatus = PartiallyPaid,
                  paymentType = StandardPayment,
                  paymentDocument = "clearing-document",
                  refundAmount = 0
                )
              ),
              testEclReference,
              testSubscribedSubscriptionStatus
            )
          )
        )
    }

    "return empty payment history where there is no clearing reason in response" in forAll {
      validResponse: ValidFinancialDataResponseForLatestObligation =>
        val firstItem       = validResponse.financialDataResponse.documentDetails.get.head.lineItemDetails.get.head.copy(
          clearingReason = None
        )
        val documentDetails = validResponse.financialDataResponse.documentDetails.get.head

        val financialDataResponse = Some(
          validResponse.financialDataResponse.copy(
            documentDetails = Some(
              Seq(
                documentDetails.copy(lineItemDetails = Some(Seq(firstItem)))
              )
            )
          )
        )

        when(mockECLAccountConnector.getFinancialData(any()))
          .thenReturn(
            Future.successful(
              financialDataResponse
            )
          )

        await(
          service.prepareViewModel(financialDataResponse, testEclReference, testSubscribedSubscriptionStatus).value
        ) shouldBe Right(
          Some(
            PaymentsViewModel(
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
              paymentHistory = Seq.empty,
              testEclReference,
              testSubscribedSubscriptionStatus
            )
          )
        )
    }

    "return Some with PaymentsViewModel with interest that is not yet formed into interest document" in forAll {
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

        when(mockECLAccountConnector.getFinancialData(any()))
          .thenReturn(Future.successful(Some(validFinancialDataResponse)))

        val documentDetails = validResponse.financialDataResponse.documentDetails.get.head
        val firstItem       = validResponse.financialDataResponse.documentDetails.get.head.lineItemDetails.get.head
        await(
          service
            .prepareViewModel(Some(validFinancialDataResponse), testEclReference, testSubscribedSubscriptionStatus)
            .value
        ) shouldBe Right(
          Some(
            PaymentsViewModel(
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
              ),
              testEclReference,
              testSubscribedSubscriptionStatus
            )
          )
        )
    }
  }

  "retrieveFinancialData" should {
    "return Left with EclAccountError-InternalServerError when connector fails with 4xx error" in {
      val errorCode    = BAD_REQUEST
      val errorMessage = "ErrorMessage"

      when(mockECLAccountConnector.getFinancialData(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse(errorMessage, errorCode)))

      val result = await(service.retrieveFinancialData(any()).value)

      result shouldBe Left(EclAccountError.BadGateway(s"Get Financial Data Failed - $errorMessage", errorCode))

    }

    "return Left with EclAccountError-InternalUnexpectedError when connector fails with an unexpected error" in {
      val errorMessage         = "ErrorMessage"
      val throwable: Exception = new Exception(errorMessage)

      when(mockECLAccountConnector.getFinancialData(any()))
        .thenReturn(Future.failed(throwable))

      val result = await(service.retrieveFinancialData(any()).value)

      result shouldBe Left(
        EclAccountError.InternalUnexpectedError(errorMessage, Some(throwable))
      )

    }
  }

  "retrieveObligationData" should {
    "return Left with EclAccountError-InternalServerError when connector fails with 4xx error" in {
      val errorCode    = BAD_REQUEST
      val errorMessage = "ErrorMessage"

      when(mockECLAccountConnector.getObligationData(any()))
        .thenReturn(Future.failed(UpstreamErrorResponse(errorMessage, errorCode)))

      val result = await(service.retrieveObligationData(any()).value)

      result shouldBe Left(EclAccountError.BadGateway(s"Get Obligation Data Failed - $errorMessage", errorCode))

    }

    "return Left with EclAccountError-InternalUnexpectedError when connector fails with an unexpected error" in {
      val errMessage           = "ErrorMessage"
      val throwable: Exception = new Exception(errMessage)

      when(mockECLAccountConnector.getObligationData(any()))
        .thenReturn(Future.failed(throwable))

      val result = await(service.retrieveObligationData(any()).value)

      result shouldBe Left(EclAccountError.InternalUnexpectedError(errMessage, Some(throwable)))

    }
  }
}
