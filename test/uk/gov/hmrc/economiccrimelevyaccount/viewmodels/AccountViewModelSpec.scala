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
import play.twirl.api.Html
import uk.gov.hmrc.economiccrimelevyaccount.ObligationDataWithObligation
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.config.AppConfig
import org.mockito.Mockito.{reset, times, verify, when}
import uk.gov.hmrc.economiccrimelevyaccount.models.{ObligationDetails, Open}
import java.time.LocalDate

class AccountViewModelSpec extends SpecBase {

  override def moduleOverrides(): Seq[GuiceableModule] = Seq(
    bind[AppConfig].toInstance(mock[AppConfig])
  )

  override def beforeEach(): Unit =
    reset(appConfig)

  private def obligationFor(
    fromYear: Int,
    toYear: Int
  ): ObligationDetails =
    ObligationDetails(
      status = Open,
      inboundCorrespondenceFromDate = LocalDate.of(fromYear, 4, 1),
      inboundCorrespondenceToDate = LocalDate.of(toYear, 3, 31),
      inboundCorrespondenceDateReceived = None,
      inboundCorrespondenceDueDate = LocalDate.of(toYear, 9, 30),
      periodKey = s"${fromYear.toString.takeRight(2)}XY"
    )

  "canAmendRegistration" should {
    "return true when amendRegistrationEnabled is true and subscribed" in {
      when(appConfig.amendRegistrationEnabled)
        .thenReturn(true)

      val sut =
        AccountViewModel(appConfig, testSubscribedSubscriptionStatus, testEclReference, None, None)

      sut.canAmendRegistration shouldBe true
    }

    "return false when amendRegistrationEnabled is true and deregistered" in {
      when(appConfig.amendRegistrationEnabled)
        .thenReturn(true)

      val sut = AccountViewModel(
        appConfig,
        testDeregisteredSubscriptionStatus,
        testEclReference,
        None,
        None
      )

      sut.canAmendRegistration shouldBe false
    }

    "return false when amendRegistrationEnabled is false and subscribed" in {
      when(appConfig.amendRegistrationEnabled)
        .thenReturn(false)

      val sut =
        AccountViewModel(appConfig, testSubscribedSubscriptionStatus, testEclReference, None, None)

      sut.canAmendRegistration shouldBe false
    }

    "return false when amendRegistrationEnabled is false and deregistered" in {
      when(appConfig.amendRegistrationEnabled)
        .thenReturn(false)

      val sut = AccountViewModel(
        appConfig,
        testDeregisteredSubscriptionStatus,
        testEclReference,
        None,
        None
      )

      sut.canAmendRegistration shouldBe false
    }
  }

  "canViewPayments" should {
    "return true when paymentsEnabled is true" in {
      when(appConfig.paymentsEnabled)
        .thenReturn(true)

      val sut =
        AccountViewModel(appConfig, testSubscribedSubscriptionStatus, testEclReference, None, None)

      sut.canViewPayments shouldBe true
    }

    "return false when paymentsEnabled is false" in {
      when(appConfig.paymentsEnabled)
        .thenReturn(false)

      val sut = AccountViewModel(
        appConfig,
        testDeregisteredSubscriptionStatus,
        testEclReference,
        None,
        None
      )

      sut.canViewPayments shouldBe false
    }
  }

  "canViewReturns" should {
    "return true when returnsEnabled is true" in {
      when(appConfig.returnsEnabled)
        .thenReturn(true)

      val sut =
        AccountViewModel(appConfig, testSubscribedSubscriptionStatus, testEclReference, None, None)

      sut.canViewReturns shouldBe true
    }

    "return false when returnsEnabled is false" in {
      when(appConfig.returnsEnabled)
        .thenReturn(false)

      val sut =
        AccountViewModel(
          appConfig,
          testDeregisteredSubscriptionStatus,
          testEclReference,
          None,
          None
        )

      sut.canViewReturns shouldBe false
    }
  }

  "isDeRegistered" should {
    "return true when subscriptionStatus is deregistered" in {
      val sut = AccountViewModel(
        appConfig,
        testDeregisteredSubscriptionStatus,
        testEclReference,
        None,
        None
      )

      sut.isDeRegistered shouldBe true
    }

    "return false when subscriptionStatus is subscribed" in {
      val sut =
        AccountViewModel(appConfig, testSubscribedSubscriptionStatus, testEclReference, None, None)

      sut.isDeRegistered shouldBe false
    }
  }

  "isSubscribed" should {
    "return true when subscriptionStatus is subscribed" in {
      val sut =
        AccountViewModel(appConfig, testSubscribedSubscriptionStatus, testEclReference, None, None)

      sut.isSubscribed shouldBe true
    }

    "return false when subscriptionStatus is deregistered" in {
      val sut = AccountViewModel(
        appConfig,
        testDeregisteredSubscriptionStatus,
        testEclReference,
        None,
        None
      )

      sut.isSubscribed shouldBe false
    }
  }

  "paymentsActions" should {
    "return multiple rows when paymentsEnabled is true and subscribed" in {
      when(appConfig.paymentsEnabled)
        .thenReturn(true)

      val sut = AccountViewModel(
        appConfig,
        testSubscribedSubscriptionStatus,
        testEclReference,
        None,
        None
      )

      val actions: Seq[CardAction] = sut.paymentsActions()(messages)

      actions.size should be > 1
    }

    "return single row if paymentsEnabled is false and subscribed" in {
      when(appConfig.paymentsEnabled)
        .thenReturn(false)

      val sut = AccountViewModel(
        appConfig,
        testSubscribedSubscriptionStatus,
        testEclReference,
        None,
        None
      )

      val actions: Seq[CardAction] = sut.paymentsActions()(messages)

      actions should have size 1
    }

    "return single row if paymentsEnabled is false and deregistered" in {
      when(appConfig.paymentsEnabled)
        .thenReturn(false)

      val sut = AccountViewModel(
        appConfig,
        testDeregisteredSubscriptionStatus,
        testEclReference,
        None,
        None
      )

      val actions: Seq[CardAction] = sut.paymentsActions()(messages)

      actions should have size 1
    }

    "return single row if paymentsEnabled is true and deregistered" in {
      when(appConfig.paymentsEnabled)
        .thenReturn(false)

      val sut = AccountViewModel(
        appConfig,
        testDeregisteredSubscriptionStatus,
        testEclReference,
        None,
        None
      )

      val actions: Seq[CardAction] = sut.paymentsActions()(messages)

      actions should have size 1
    }
  }

  "paymentsSubHeading" should {
    "return no due payments content if there are no financial details" in {
      val sut = AccountViewModel(
        appConfig,
        testSubscribedSubscriptionStatus,
        testEclReference,
        None,
        None
      )

      val subHeading: Html = sut.paymentsSubHeading()(messages)

      subHeading.body should startWith("You have no payments due")
    }
  }

  "registrationAction" should {
    "return two rows when amendRegistrationEnabled and deregisterEnabled are true and subscribed" in {
      when(appConfig.amendRegistrationEnabled)
        .thenReturn(true)

      when(appConfig.deregisterEnabled)
        .thenReturn(true)

      val sut = AccountViewModel(
        appConfig,
        testSubscribedSubscriptionStatus,
        testEclReference,
        None,
        None
      )

      val action: Seq[CardAction] = sut.registrationActions()(messages)

      action should have size 2
    }

    "return one row when amendRegistrationEnabled os true, deregisterEnabled is false and subscribed" in {
      when(appConfig.amendRegistrationEnabled)
        .thenReturn(true)

      when(appConfig.deregisterEnabled)
        .thenReturn(false)

      val sut = AccountViewModel(
        appConfig,
        testSubscribedSubscriptionStatus,
        testEclReference,
        None,
        None
      )

      val action: Seq[CardAction] = sut.registrationActions()(messages)

      action should have size 1
    }

    "return one row when amendRegistrationEnabled os false, deregisterEnabled is true and subscribed" in {
      when(appConfig.amendRegistrationEnabled)
        .thenReturn(false)

      when(appConfig.deregisterEnabled)
        .thenReturn(true)

      val sut = AccountViewModel(
        appConfig,
        testSubscribedSubscriptionStatus,
        testEclReference,
        None,
        None
      )

      val action: Seq[CardAction] = sut.registrationActions()(messages)

      action should have size 1
    }

    "return no rows when amendRegistrationEnabled and deregisterEnabled are false and subscribed" in {
      when(appConfig.amendRegistrationEnabled)
        .thenReturn(false)

      when(appConfig.deregisterEnabled)
        .thenReturn(false)

      val sut = AccountViewModel(
        appConfig,
        testSubscribedSubscriptionStatus,
        testEclReference,
        None,
        None
      )

      val action: Seq[CardAction] = sut.registrationActions()(messages)

      action should have size 0
    }

    "return no rows when amendRegistrationEnabled is true and deregistered" in {
      when(appConfig.amendRegistrationEnabled)
        .thenReturn(false)

      val sut = AccountViewModel(
        appConfig,
        testDeregisteredSubscriptionStatus,
        testEclReference,
        None,
        None
      )

      val action: Seq[CardAction] = sut.registrationActions()(messages)

      action should have size 0
    }

    "return no rows when amendRegistrationEnabled is false and deregistered" in {
      when(appConfig.amendRegistrationEnabled)
        .thenReturn(false)

      val sut = AccountViewModel(
        appConfig,
        testDeregisteredSubscriptionStatus,
        testEclReference,
        None,
        None
      )

      val action: Seq[CardAction] = sut.registrationActions()(messages)

      action should have size 0
    }

    "not return the submit return action when the return is prevented" in {
      when(appConfig.preventReturnSubmissionEnabled)
        .thenReturn(true)

      when(appConfig.preventedReturnTaxYears)
        .thenReturn(Seq("2026-2027"))

      val sut = AccountViewModel(
        appConfig,
        testSubscribedSubscriptionStatus,
        testEclReference,
        Some(obligationFor(2026, 2027)),
        None
      )

      val actions = sut.returnsActions()(messages)

      actions should have size 2
    }

    "return the submit return action when prevention is disabled" in {
      when(appConfig.preventReturnSubmissionEnabled)
        .thenReturn(false)

      when(appConfig.preventedReturnTaxYears)
        .thenReturn(Seq("2026-2027"))

      val sut = AccountViewModel(
        appConfig,
        testSubscribedSubscriptionStatus,
        testEclReference,
        Some(obligationFor(2026, 2027)),
        None
      )

      val actions = sut.returnsActions()(messages)

      actions should have size 3
    }
  }

  "returnsActions" should {
    "return multiple rows when subscribed" in {
      val sut = AccountViewModel(
        appConfig,
        testSubscribedSubscriptionStatus,
        testEclReference,
        None,
        None
      )

      val actions: Seq[CardAction] = sut.returnsActions()(messages)

      actions.size should be > 1
    }
  }

  "returnsSubHeading" should {
    "return overdue return content if overdue and subscribed" in forAll {
      (obligationData: ObligationDataWithObligation) =>
        val sut = AccountViewModel(
          appConfig,
          testSubscribedSubscriptionStatus,
          testEclReference,
          obligationData.obligationData.latestObligation,
          None
        )

        val subHeading: Html = sut.returnsSubHeading()(messages)

        subHeading.body should startWith("Your return for")
    }

    "return due return content if due and subscribed" in forAll { (obligationData: ObligationDataWithObligation) =>
      val sut = AccountViewModel(
        appConfig,
        testSubscribedSubscriptionStatus,
        testEclReference,
        obligationData.obligationData.latestObligation,
        None
      )

      val subHeading: Html = sut.returnsSubHeading()(messages)

      subHeading.body should startWith("Your return for")
    }

    "return no return due content if there are no obligations" in {
      val sut = AccountViewModel(
        appConfig,
        testSubscribedSubscriptionStatus,
        testEclReference,
        None,
        None
      )

      val subHeading: Html = sut.returnsSubHeading()(messages)

      subHeading.body should startWith("You have no returns due")
    }

    "return prevented return content with the correct available from date" in {
      when(appConfig.preventReturnSubmissionEnabled)
        .thenReturn(true)

      when(appConfig.preventedReturnTaxYears)
        .thenReturn(Seq("2026-2027"))

      val sut = AccountViewModel(
        appConfig,
        testSubscribedSubscriptionStatus,
        testEclReference,
        Some(obligationFor(2026, 2027)),
        None
      )

      val subHeading: Html = sut.returnsSubHeading()(messages)

      subHeading.body shouldBe
        "You cannot submit your next return yet. It will be available from 1 April 2027."
    }
  }

  "canDeregister" should {
    "return true when deregisterEnabled is true and subscribed" in {
      when(appConfig.deregisterEnabled)
        .thenReturn(true)

      val sut =
        AccountViewModel(appConfig, testSubscribedSubscriptionStatus, testEclReference, None, None)

      sut.canDeregister shouldBe true
    }

    "return false when deregisterEnabled is true and deregistered" in {
      when(appConfig.deregisterEnabled)
        .thenReturn(true)

      val sut = AccountViewModel(
        appConfig,
        testDeregisteredSubscriptionStatus,
        testEclReference,
        None,
        None
      )

      sut.canDeregister shouldBe false
    }

    "return false when deregisterEnabled is false and subscribed" in {
      when(appConfig.deregisterEnabled)
        .thenReturn(false)

      val sut =
        AccountViewModel(appConfig, testSubscribedSubscriptionStatus, testEclReference, None, None)

      sut.canDeregister shouldBe false
    }

    "return false when deregisterEnabled is false and deregistered" in {
      when(appConfig.deregisterEnabled)
        .thenReturn(false)

      val sut = AccountViewModel(
        appConfig,
        testDeregisteredSubscriptionStatus,
        testEclReference,
        None,
        None
      )

      sut.canDeregister shouldBe false
    }
  }

  "canViewRegistration" should {
    "return true when subscribed and amendRegistrationEnabled is true and deregisterEnabled is false" in {
      when(appConfig.deregisterEnabled)
        .thenReturn(false)

      when(appConfig.amendRegistrationEnabled)
        .thenReturn(true)

      val sut = AccountViewModel(
        appConfig,
        testSubscribedSubscriptionStatus,
        testEclReference,
        None,
        None
      )

      sut.canViewRegistration shouldBe true
    }

    "return true when subscribed and amendRegistrationEnabled is false and deregisterEnabled is true" in {
      when(appConfig.deregisterEnabled)
        .thenReturn(true)

      when(appConfig.amendRegistrationEnabled)
        .thenReturn(false)

      val sut = AccountViewModel(
        appConfig,
        testSubscribedSubscriptionStatus,
        testEclReference,
        None,
        None
      )

      sut.canViewRegistration shouldBe true
    }

    "return true when deregistered and canAmendRegistration is true and canDeregister is false" in {
      when(appConfig.deregisterEnabled)
        .thenReturn(false)

      when(appConfig.amendRegistrationEnabled)
        .thenReturn(true)

      val sut = AccountViewModel(
        appConfig,
        testDeregisteredSubscriptionStatus,
        testEclReference,
        None,
        None
      )

      sut.canViewRegistration shouldBe false
    }

    "return true when deregistered and canAmendRegistration is false and canDeregister is true" in {
      when(appConfig.deregisterEnabled)
        .thenReturn(true)

      when(appConfig.amendRegistrationEnabled)
        .thenReturn(false)

      val sut = AccountViewModel(
        appConfig,
        testDeregisteredSubscriptionStatus,
        testEclReference,
        None,
        None
      )

      sut.canViewRegistration shouldBe false
    }

    "isReturnPrevented" should {

      "return true when prevention is enabled and the obligation tax year is configured for prevention" in {
        when(appConfig.preventReturnSubmissionEnabled)
          .thenReturn(true)

        when(appConfig.preventedReturnTaxYears)
          .thenReturn(Seq("2026-2027"))

        val sut = AccountViewModel(
          appConfig,
          testSubscribedSubscriptionStatus,
          testEclReference,
          Some(obligationFor(2026, 2027)),
          None
        )

        sut.isReturnPrevented shouldBe true
      }

      "return false when prevention is disabled" in {
        when(appConfig.preventReturnSubmissionEnabled)
          .thenReturn(false)

        when(appConfig.preventedReturnTaxYears)
          .thenReturn(Seq("2026-2027"))

        val sut = AccountViewModel(
          appConfig,
          testSubscribedSubscriptionStatus,
          testEclReference,
          Some(obligationFor(2026, 2027)),
          None
        )

        sut.isReturnPrevented shouldBe false
      }

      "return false when the obligation tax year is not configured for prevention" in {
        when(appConfig.preventReturnSubmissionEnabled)
          .thenReturn(true)

        when(appConfig.preventedReturnTaxYears)
          .thenReturn(Seq("2026-2027"))

        val sut = AccountViewModel(
          appConfig,
          testSubscribedSubscriptionStatus,
          testEclReference,
          Some(obligationFor(2025, 2026)),
          None
        )

        sut.isReturnPrevented shouldBe false
      }

      "support future tax years configured for prevention" in {
        when(appConfig.preventReturnSubmissionEnabled)
          .thenReturn(true)

        when(appConfig.preventedReturnTaxYears)
          .thenReturn(Seq("2029-2030"))

        val sut = AccountViewModel(
          appConfig,
          testSubscribedSubscriptionStatus,
          testEclReference,
          Some(obligationFor(2029, 2030)),
          None
        )

        sut.isReturnPrevented shouldBe true
      }
    }
  }
}
