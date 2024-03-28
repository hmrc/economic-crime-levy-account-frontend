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

package uk.gov.hmrc.economiccrimelevyaccount.utils

trait Constants

object DocumentType extends Constants {
  val interestDocument = "Interest Document"
  val payment          = "Payment"
  val trmAmendCharge   = "TRM Amend Charge"
  val trmNewCharge     = "TRM New Charge"
}

object HttpHeader extends Constants {
  val xCorrelationId = "x-correlation-id"
  val xSessionId     = "x-session-id"
}

object Constants {
  val ecl = "ECL"
}
