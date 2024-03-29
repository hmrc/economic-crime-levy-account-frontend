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

package uk.gov.hmrc.economiccrimelevyaccount.controllers

import play.api.Logging

import scala.concurrent.{ExecutionContext, Future}
import cats.data.EitherT
import uk.gov.hmrc.economiccrimelevyaccount.models.errors._

trait ErrorHandler extends Logging {

  implicit class ErrorConvertor[E, R](value: EitherT[Future, E, R]) {

    def asResponseError(implicit c: Converter[E], ec: ExecutionContext): EitherT[Future, ResponseError, R] =
      value.leftMap(c.convert).leftSemiflatTap {
        case InternalServiceError(message, _, cause) =>
          val causeText = cause
            .map { ex =>
              s"""
                   |Message: ${ex.getMessage}
                   |""".stripMargin
            }
            .getOrElse("No exception is available")
          logger.error(s"""Internal Server Error: $message
               |
               |$causeText""".stripMargin)
          Future.successful(())
        case BadGateway(message, _, responseCode)    =>
          val causeText = s"""
                 |Message: $message
                 |Upstream status code: $responseCode
                 |""".stripMargin

          logger.error(s"""Bad gateway: $message
               |
               |$causeText""".stripMargin)
          Future.successful(())
        case _                                       => Future.successful(())
      }
  }

  trait Converter[E] {
    def convert(error: E): ResponseError
  }

  implicit val eclAccountErrorConverter: Converter[EclAccountError] = {
    case EclAccountError.BadGateway(cause, statusCode)           => ResponseError.badGateway(cause, statusCode)
    case EclAccountError.InternalUnexpectedError(message, cause) =>
      ResponseError.internalServiceError(message = message, cause = cause)
  }

  implicit val opsErrorConverter: Converter[OpsError] = {
    case OpsError.BadGateway(cause, statusCode)           => ResponseError.badGateway(cause, statusCode)
    case OpsError.InternalUnexpectedError(message, cause) =>
      ResponseError.internalServiceError(message = message, cause = cause)
  }

  implicit val eclRegistrationErrorConverter: Converter[EclRegistrationError] = {
    case EclRegistrationError.BadGateway(cause, statusCode)           => ResponseError.badGateway(cause, statusCode)
    case EclRegistrationError.InternalUnexpectedError(message, cause) =>
      ResponseError.internalServiceError(message = message, cause = cause)
  }
}
