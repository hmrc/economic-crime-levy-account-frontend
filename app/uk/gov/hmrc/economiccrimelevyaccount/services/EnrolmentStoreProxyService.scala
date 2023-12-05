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

import cats.data.EitherT
import uk.gov.hmrc.economiccrimelevyaccount.connectors.EnrolmentStoreProxyConnector
import uk.gov.hmrc.economiccrimelevyaccount.models.EclReference
import uk.gov.hmrc.economiccrimelevyaccount.models.errors.EnrolmentStoreError
import uk.gov.hmrc.http.{HeaderCarrier, UpstreamErrorResponse}

import java.time.LocalDate
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}
import scala.util.control.NonFatal

class EnrolmentStoreProxyService @Inject() (enrolmentStoreProxyConnector: EnrolmentStoreProxyConnector)(implicit
  ec: ExecutionContext
) {

  def getEclRegistrationDate(
    eclReference: EclReference
  )(implicit hc: HeaderCarrier): EitherT[Future, EnrolmentStoreError, LocalDate] =
    EitherT {
      enrolmentStoreProxyConnector
        .getEnrolments(eclReference)
        .map(_.getRegistrationDate)
        .recover {
          case error @ UpstreamErrorResponse(message, code, _, _)
              if UpstreamErrorResponse.Upstream5xxResponse
                .unapply(error)
                .isDefined || UpstreamErrorResponse.Upstream4xxResponse.unapply(error).isDefined =>
            Left(EnrolmentStoreError.BadGateway(reason = message, code = code))
          case NonFatal(thr) => Left(EnrolmentStoreError.InternalUnexpectedError(thr.getMessage, Some(thr)))
        }
    }

}
