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

package uk.gov.hmrc.economiccrimelevyaccount.viewmodels

import play.api.inject.bind
import play.api.inject.guice.GuiceableModule
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.config.AppConfig

class AccountViewModelSpec extends SpecBase {

  override def moduleOverrides(): Seq[GuiceableModule] = Seq(
    bind[AppConfig].toInstance(mock[AppConfig])
  )

  override def beforeEach(): Unit = {
    reset(appConfig)
  }

  "canAmendRegistration" should {
    "return true when amendRegistrationEnabled is true and subscribed" in forAll {
      (eclRegistrationDate: String) =>

        when(appConfig.amendRegistrationEnabled)
          .thenReturn(true)

        val sut = AccountViewModel(
          appConfig,
          testSubscribedSubscriptionStatus,
          testEclReference,
          eclRegistrationDate,
          None,
          None)

        sut.canAmendRegistration shouldBe true
    }

    "return false when amendRegistrationEnabled is true and deregistered" in forAll {
      (eclRegistrationDate: String) =>

        when(appConfig.amendRegistrationEnabled)
          .thenReturn(true)

        val sut = AccountViewModel(
          appConfig,
          testDeregisteredSubscriptionStatus,
          testEclReference,
          eclRegistrationDate,
          None,
          None)

        sut.canAmendRegistration shouldBe false
    }

    "return false when amendRegistrationEnabled is false and subscribed" in forAll {
      (eclRegistrationDate: String) =>

        when(appConfig.amendRegistrationEnabled)
          .thenReturn(false)

        val sut = AccountViewModel(
          appConfig,
          testSubscribedSubscriptionStatus,
          testEclReference,
          eclRegistrationDate,
          None,
          None)

        sut.canAmendRegistration shouldBe false
    }

    "return false when amendRegistrationEnabled is false and deregistered" in forAll {
      (eclRegistrationDate: String) =>

        when(appConfig.amendRegistrationEnabled)
          .thenReturn(false)

        val sut = AccountViewModel(
          appConfig,
          testDeregisteredSubscriptionStatus,
          testEclReference,
          eclRegistrationDate,
          None,
          None)

        sut.canAmendRegistration shouldBe false
    }
  }

  "canViewPayments" should {
    "return true when paymentsEnabled is true" in forAll {
      (eclRegistrationDate: String) =>

        when(appConfig.paymentsEnabled)
          .thenReturn(true)

        val sut = AccountViewModel(
          appConfig,
          testSubscribedSubscriptionStatus,
          testEclReference,
          eclRegistrationDate,
          None,
          None)

        sut.canViewPayments shouldBe true
    }

    "return false when paymentsEnabled is false" in forAll {
      (eclRegistrationDate: String) =>

        when(appConfig.paymentsEnabled)
          .thenReturn(false)

        val sut = AccountViewModel(
          appConfig,
          testDeregisteredSubscriptionStatus,
          testEclReference,
          eclRegistrationDate,
          None,
          None)

        sut.canViewPayments shouldBe false
    }
  }

  "canViewReturns" should {
    "return true when returnsEnabled is true" in forAll {
      (eclRegistrationDate: String) =>

        when(appConfig.returnsEnabled)
          .thenReturn(true)

        val sut = AccountViewModel(
          appConfig,
          testSubscribedSubscriptionStatus,
          testEclReference,
          eclRegistrationDate,
          None,
          None)

        sut.canViewReturns shouldBe true
    }

    "return false when returnsEnabled is false" in forAll {
      (eclRegistrationDate: String) =>

        when(appConfig.returnsEnabled)
          .thenReturn(false)

        val sut = AccountViewModel(
          appConfig,
          testDeregisteredSubscriptionStatus,
          testEclReference,
          eclRegistrationDate,
          None,
          None)

        sut.canViewReturns shouldBe false
    }
  }

  "isDeRegistered" should {
    "return true when subscriptionStatus is deregistered" in forAll {
      (eclRegistrationDate: String) =>

      val sut = AccountViewModel(
        appConfig,
        testDeregisteredSubscriptionStatus,
        testEclReference,
        eclRegistrationDate,
        None,
        None)

        sut.isDeRegistered shouldBe true
    }

    "return false when subscriptionStatus is subscribed" in forAll {
      (eclRegistrationDate: String) =>

        val sut = AccountViewModel(
          appConfig,
          testSubscribedSubscriptionStatus,
          testEclReference,
          eclRegistrationDate,
          None,
          None)

        sut.isDeRegistered shouldBe false
    }
  }

  "isSubscribed" should {
    "return true when subscriptionStatus is subscribed" in forAll {
      (eclRegistrationDate: String) =>

        val sut = AccountViewModel(
          appConfig,
          testSubscribedSubscriptionStatus,
          testEclReference,
          eclRegistrationDate,
          None,
          None)

        sut.isSubscribed shouldBe true
    }

    "return false when subscriptionStatus is deregistered" in forAll {
      (eclRegistrationDate: String) =>

        val sut = AccountViewModel(
          appConfig,
          testDeregisteredSubscriptionStatus,
          testEclReference,
          eclRegistrationDate,
          None,
          None)

        sut.isSubscribed shouldBe false
    }
  }

  "paymentsActions" should {
    "return single row if paymentsEnabled is false" in forAll {
      (eclRegistrationDate: String) =>

        when(appConfig.paymentsEnabled)
          .thenReturn(false)

        val sut = AccountViewModel(
          appConfig,
          testDeregisteredSubscriptionStatus,
          testEclReference,
          eclRegistrationDate,
          None,
          None)

        sut.paymentsActions()(messages).size shouldBe 1
    }
  }
}
