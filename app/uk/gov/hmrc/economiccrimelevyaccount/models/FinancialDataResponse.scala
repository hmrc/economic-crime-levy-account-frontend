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

package uk.gov.hmrc.economiccrimelevyaccount.models

import play.api.http.Status.{INTERNAL_SERVER_ERROR, OK}
import play.api.libs.json._
import uk.gov.hmrc.http.{HttpReads, HttpResponse}
case class FinancialDataResponse(totalisation: Option[Totalisation], documentDetails: Option[Seq[DocumentDetails]])

object FinancialDataResponse {
  implicit object FinancialDataResponseReads
      extends HttpReads[Either[FinancialDataErrorResponse, FinancialDataResponse]] {
    override def read(
      method: String,
      url: String,
      response: HttpResponse
    ): Either[FinancialDataErrorResponse, FinancialDataResponse] =
      response.status match {
        case OK                    =>
          response.json.validate[FinancialDataResponse] match {
            case JsSuccess(response, _) => Right(response)
            case JsError(errors)        =>
              Left(
                FinancialDataErrorResponse(
                  Some(INTERNAL_SERVER_ERROR.toString),
                  Some(errors.flatMap(_._2).mkString(","))
                )
              )
          }
        case INTERNAL_SERVER_ERROR =>
          response.json.validate[FinancialDataErrorResponse] match {
            case JsSuccess(errorResponse, _) => Left(errorResponse)
            case JsError(errors)             =>
              Left(
                FinancialDataErrorResponse(
                  Some(INTERNAL_SERVER_ERROR.toString),
                  Some(errors.flatMap(_._2).mkString(","))
                )
              )
          }
      }
  }

  implicit val format: OFormat[FinancialDataResponse] = Json.format[FinancialDataResponse]
}

case class Totalisation(
  totalOverdue: Option[BigDecimal],
  totalNotYetDue: Option[BigDecimal],
  totalBalance: Option[BigDecimal],
  totalCredit: Option[BigDecimal],
  totalCleared: Option[BigDecimal]
)

object Totalisation {
  implicit val format: OFormat[Totalisation] = Json.format[Totalisation]
}

case class DocumentDetails(
  documentType: Option[FinancialDataDocumentType],
  chargeReferenceNumber: Option[String],
  postingDate: Option[String],
  issueDate: Option[String],
  documentTotalAmount: Option[BigDecimal],
  documentClearedAmount: Option[BigDecimal],
  documentOutstandingAmount: Option[BigDecimal],
  lineItemDetails: Option[Seq[LineItemDetails]]
) {
  val isCleared: Boolean = documentOutstandingAmount match {
    case None        => throw new IllegalStateException()
    case Some(value) => value <= 0
  }
}
object DocumentDetails {
  implicit val format: OFormat[DocumentDetails] = Json.format[DocumentDetails]
}

sealed trait FinancialDataDocumentType

object FinancialDataDocumentType {
  implicit val format: Format[FinancialDataDocumentType] = new Format[FinancialDataDocumentType] {
    override def reads(json: JsValue): JsResult[FinancialDataDocumentType] = json.validate[String] match {
      case JsSuccess(value, _) =>
        value match {
          case "TRM New Charge"      => JsSuccess(NewCharge)
          case "TRM Amended Charge"  => JsSuccess(AmendedCharge)
          case "TRM Reversed Charge" => JsSuccess(ReversedCharge)
          case _                     => JsError("Invalid charge type has been passed")
        }
      case e: JsError          => e
    }

    override def writes(o: FinancialDataDocumentType): JsValue = o match {
      case NewCharge      => JsString("TRM New Charge")
      case AmendedCharge  => JsString("TRM Amended Charge")
      case ReversedCharge => JsString("TRM Reversed Charge")
    }
  }
}

case object NewCharge extends FinancialDataDocumentType

case object AmendedCharge extends FinancialDataDocumentType

case object ReversedCharge extends FinancialDataDocumentType

case class LineItemDetails(
  chargeDescription: Option[String],
  periodFromDate: Option[String],
  periodToDate: Option[String],
  periodKey: Option[String],
  netDueDate: Option[String],
  amount: Option[BigDecimal],
  clearingDate: Option[String]
)

object LineItemDetails {
  implicit val format: OFormat[LineItemDetails] = Json.format[LineItemDetails]
}
