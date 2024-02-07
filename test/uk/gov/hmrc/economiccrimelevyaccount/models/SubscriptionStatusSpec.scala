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

package uk.gov.hmrc.economiccrimelevyaccount.models

import play.api.libs.json._
import uk.gov.hmrc.economiccrimelevyaccount.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.models.EclSubscriptionStatus._

class SubscriptionStatusSpec extends SpecBase {

  "writes" should {
    "return the subscription status serialized to its JSON representation" in forAll(
      Table(
        ("subscriptionStatus", "expectedResult"),
        (
          Subscribed(testEclRegistrationReference),
          Json.obj("status" -> "Subscribed", "eclRegistrationReference" -> testEclRegistrationReference)
        ),
        (NotSubscribed, JsString("NotSubscribed"))
      )
    ) { (subscriptionStatus: SubscriptionStatus, expectedResult: JsValue) =>
      val result: JsValue = Json.toJson(subscriptionStatus)(EclSubscriptionStatus.subscriptionStatusFormat)

      result shouldBe expectedResult
    }
  }

  "reads" should {
    "return the subscription status deserialized from its JSON representation" in forAll {
      (subscriptionStatus: SubscriptionStatus) =>
        val json: JsValue = Json.toJson(subscriptionStatus)(EclSubscriptionStatus.subscriptionStatusFormat)

        val result: SubscriptionStatus = json.as[SubscriptionStatus](EclSubscriptionStatus.subscriptionStatusFormat)

        result shouldBe subscriptionStatus
    }

    "return a JsError when passed an invalid string value" in {
      val result: JsResult[SubscriptionStatus] =
        Json.fromJson[SubscriptionStatus](JsString("Test"))(EclSubscriptionStatus.subscriptionStatusFormat)

      result shouldBe JsError(s"Test is not a valid SubscriptionStatus")
    }

    "return a JsError when passed a json object that does not contain a Subscribed status with an ECL registration reference" in {
      val json: JsObject = Json.obj("Test" -> "Test")

      val result: JsResult[SubscriptionStatus] =
        Json.fromJson[SubscriptionStatus](json)(
          EclSubscriptionStatus.subscriptionStatusFormat
        )

      result shouldBe JsError(s"$json is not a valid SubscriptionStatus")
    }

    "return a JsError when passed a json object that contains a status other than Subscribed with an ECL registration reference" in {
      val result: JsResult[SubscriptionStatus] =
        Json.fromJson[SubscriptionStatus](
          Json.obj("status" -> "Test", "eclRegistrationReference" -> testEclRegistrationReference)
        )(EclSubscriptionStatus.subscriptionStatusFormat)

      result shouldBe JsError("Test is not a valid SubscriptionStatus")
    }
  }
}
