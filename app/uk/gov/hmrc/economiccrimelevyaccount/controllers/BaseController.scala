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

import play.api.libs.json.{Json, Writes}
import play.api.mvc.Result
import play.api.mvc.Results.Status
import uk.gov.hmrc.economiccrimelevyaccount.models.errors.ResponseError

import scala.concurrent.{ExecutionContext, Future}
import cats.data.EitherT
trait BaseController {

  implicit class ResponseHandler[R](value: EitherT[Future, ResponseError, R]) {

    def convertToResultWithJsonBody(
      statusCode: Int
    )(implicit ec: ExecutionContext, writes: Writes[R]): Future[Result] =
      value.fold(
        err => Status(err.code.statusCode)(Json.toJson(err)),
        response => Status(statusCode)(Json.toJson(response))
      )
  }

}
