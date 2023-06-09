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

import uk.gov.hmrc.economiccrimelevyaccount.connectors.FinancialDataConnector
import uk.gov.hmrc.economiccrimelevyaccount.models.{DocumentDetails, FinancialDataErrorResponse, FinancialDataResponse, FinancialDetails, NewCharge}
import uk.gov.hmrc.http.HeaderCarrier

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class FinancialDataService @Inject() (
  financialDataConnector: FinancialDataConnector
)(implicit ec: ExecutionContext) {

  private def retrieveFinancialData(implicit
    hc: HeaderCarrier
  ): Future[Either[FinancialDataErrorResponse, FinancialDataResponse]] = financialDataConnector.getFinancialData()

  def latestFinancialObligation(implicit hc: HeaderCarrier): Future[Option[FinancialDetails]] =
    retrieveFinancialData.map {
      case Left(error)          => findLatestFinancialObligation(FinancialDataResponse(None, None))
      case Right(financialData) => findLatestFinancialObligation(financialData)
    }
  private def findLatestFinancialObligation(financialData: FinancialDataResponse): Option[FinancialDetails] = {
    val documentDetails: Option[DocumentDetails] = financialData.documentDetails match {
      case None        => None
      case Some(value) =>
        value
          .filter(docDetails => extractValue(docDetails.documentType) == NewCharge)
          .filter(!_.isCleared)
          .sortBy(_.postingDate)
          .headOption
    }

    documentDetails match {
      case None        => None
      case Some(value) =>
        val outstandingAmount           = extractValue(value.documentOutstandingAmount)
        val lineItemDetails             = extractValue(value.lineItemDetails)
        val firstLineItemDetailsElement = lineItemDetails.head

        Some(
          FinancialDetails(
            outstandingAmount,
            LocalDate.parse(extractValue(firstLineItemDetailsElement.periodFromDate)),
            LocalDate.parse(extractValue(firstLineItemDetailsElement.periodToDate)),
            extractValue(firstLineItemDetailsElement.periodKey)
          )
        )
    }
  }

  private def extractValue[A](value: Option[A]): A = value.getOrElse(throw new IllegalStateException())
}
