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

package uk.gov.hmrc.economiccrimelevyaccount.models.eacd

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.economiccrimelevyaccount.models.errors.EnrolmentStoreError

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import scala.util.{Success, Try}

final case class EnrolmentResponse(service: String, enrolments: Seq[Enrolment]) {
  def getRegistrationDate: Either[EnrolmentStoreError, LocalDate] = {
    val keyValueOption =
      enrolments.headOption.flatMap(enrolment => enrolment.verifiers.find(_.key == EclEnrolment.RegistrationDateKey))

    keyValueOption
      .map { registrationDateKeyValue =>
        Try(LocalDate.parse(registrationDateKeyValue.value, DateTimeFormatter.BASIC_ISO_DATE)) match {
          case Success(registrationDate) => Right(registrationDate)
          case _                         =>
            Left(EnrolmentStoreError.InternalUnexpectedError("Unable to parse registrationDate", None))
        }
      }
      .getOrElse(Left(EnrolmentStoreError.InternalUnexpectedError("Missing registrationDate", None)))
  }
}

object EnrolmentResponse {
  implicit val format: OFormat[EnrolmentResponse] = Json.format[EnrolmentResponse]
}
