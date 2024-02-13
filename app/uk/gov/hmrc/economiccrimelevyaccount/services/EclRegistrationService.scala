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

import cats.data.EitherT
import uk.gov.hmrc.economiccrimelevyaccount.connectors.EclRegistrationConnector
import uk.gov.hmrc.economiccrimelevyaccount.models.{EclReference, EclSubscriptionStatus}
import uk.gov.hmrc.economiccrimelevyaccount.models.errors.EclRegistrationError
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

@Singleton
class EclRegistrationService @Inject() (
  eclRegistrationConnector: EclRegistrationConnector
)(implicit ec: ExecutionContext) {

  def getSubscriptionStatus(
    eclRegistrationReference: EclReference
  )(implicit hc: HeaderCarrier): EitherT[Future, EclRegistrationError, EclSubscriptionStatus] =
    EitherT {
      eclRegistrationConnector
        .getSubscriptionStatus(eclRegistrationReference)
        .map(Right(_))
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse
                .unapply(error)
                .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            Left(EclRegistrationError.BadGateway(message, code))
          case NonFatal(thr) => Left(EclRegistrationError.InternalUnexpectedError(thr.getMessage, Some(thr)))
        }
    }
}
