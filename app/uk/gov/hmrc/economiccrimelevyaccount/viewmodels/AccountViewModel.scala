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

import play.api.i18n.Messages
import play.twirl.api.Html
import uk.gov.hmrc.economiccrimelevyaccount.config.AppConfig
import uk.gov.hmrc.economiccrimelevyaccount.controllers.routes
import uk.gov.hmrc.economiccrimelevyaccount.models.EclSubscriptionStatus.{DeRegistered, Subscribed}
import uk.gov.hmrc.economiccrimelevyaccount.models.{EclReference, EclSubscriptionStatus, FinancialDetails, ObligationDetails}
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.PaymentType.StandardPayment
import uk.gov.hmrc.economiccrimelevyaccount.views.ViewUtils

final case class AccountViewModel(
  appConfig: AppConfig,
  eclSubscriptionStatus: EclSubscriptionStatus,
  eclRegistrationReference: EclReference,
  eclRegistrationDate: String,
  optOpenObligation: Option[ObligationDetails],
  optFinancialDetails: Option[FinancialDetails]
) extends ViewModelBase {

  private val yearDigitsFromRightOfLocalDate: Int = 4

  val isDeRegistered: Boolean =
    eclSubscriptionStatus.subscriptionStatus == DeRegistered(eclRegistrationReference.value)

  val isSubscribed: Boolean =
    eclSubscriptionStatus.subscriptionStatus == Subscribed(eclRegistrationReference.value)

  private val canAmendReturns: Boolean = appConfig.amendReturnsEnabled && isSubscribed
  private val canMakePayments: Boolean = appConfig.paymentsEnabled && isSubscribed

  val canAmendRegistration: Boolean = appConfig.amendRegistrationEnabled && isSubscribed
  val canDeregister: Boolean        = appConfig.deregisterEnabled && isSubscribed
  val canViewPayments: Boolean      = appConfig.paymentsEnabled
  val canViewReturns: Boolean       = appConfig.returnsEnabled

  private def getViewReturnsLinkName()(implicit messages: Messages): String =
    if (canAmendReturns) {
      messages("account.viewOrAmendReturns")
    } else {
      messages("account.viewReturns")
    }

  private def makePaymentLink: Option[String] =
    if (canMakePayments) {
      optFinancialDetails.map(_ => "/economic-crime-levy-account/make-a-payment")
    } else {
      None
    }

  private def submitReturnLink(): Option[String] =
    optOpenObligation.map(o => s"${appConfig.returnsUrl}/period/${o.periodKey}")

  def paymentsActions()(implicit messages: Messages): Seq[CardAction] =
    addIf(
      canMakePayments,
      Seq(
        makePaymentLink.map(CardAction("make-payment", _, messages("account.payments.makePayment"))).toSeq,
        Seq(
          CardAction(
            "how-to-make-payment",
            "https://www.gov.uk/guidance/pay-your-economic-crime-levy",
            messages("account.payments.howTo")
          )
        )
      ).flatten
    ).flatten ++ Seq(
      CardAction(
        "view-payment-history",
        routes.ViewYourPaymentsController.onPageLoad().url,
        messages("account.payments.viewHistory")
      )
    )

  def paymentsSubHeading()(implicit messages: Messages): Html =
    optFinancialDetails match {
      case financialData @ Some(FinancialDetails(amount, Some(fromDate), Some(toDate), _, _, _))
          if financialData.get.isPaymentType(StandardPayment) && isSubscribed =>
        if (financialData.get.isOverdue.contains(true)) {
          Html(
            messages(
              "account.overdue.payments.subHeading.1",
              ViewUtils.formatLocalDate(fromDate).takeRight(yearDigitsFromRightOfLocalDate),
              ViewUtils.formatLocalDate(toDate).takeRight(yearDigitsFromRightOfLocalDate),
              ViewUtils.formatMoney(amount)
            )
          )
        } else {
          Html(
            messages(
              "account.due.payments.subHeading",
              ViewUtils.formatMoney(amount),
              ViewUtils.formatLocalDate(financialData.get.dueDate.get).takeRight(yearDigitsFromRightOfLocalDate),
              ViewUtils.formatLocalDate(fromDate).takeRight(yearDigitsFromRightOfLocalDate),
              ViewUtils.formatLocalDate(toDate).takeRight(yearDigitsFromRightOfLocalDate)
            )
          )
        }
      case Some(financialDetails) if !financialDetails.isPaymentType(StandardPayment) && isSubscribed =>
        Html(messages("account.interest.payments.subHeading"))
      case _                                                                                          => Html(messages("account.noneDue.payments.subHeading"))
    }

  def registrationAction()(implicit messages: Messages): Seq[CardAction] =
    addIf(
      canAmendRegistration,
      CardAction(
        "amend-registration",
        s"${appConfig.registrationUrl}/amend-economic-crime-levy-registration/${eclRegistrationReference.value}",
        messages("account.registration.card.amendRegistration")
      )
    ) ++ addIf(
      canDeregister,
      CardAction(
        "deregister",
        s"${appConfig.registrationUrl}/deregister-start",
        messages("account.registration.card.deregister")
      )
    )

  def returnsActions()(implicit messages: Messages): Seq[CardAction] =
    addIf(
      isSubscribed,
      Seq(
        submitReturnLink().map(CardAction("submit-return", _, messages("account.submitEcl"))).toSeq,
        Seq(
          CardAction(
            "how-to-complete-return",
            "https://www.gov.uk/guidance/submit-a-return-for-the-economic-crime-levy",
            messages("account.howToComplete")
          )
        )
      ).flatten
    ).flatten ++
      add(
        CardAction("view-returns", routes.ViewYourReturnsController.onPageLoad().url, getViewReturnsLinkName())
      )

  def returnsSubHeading()(implicit messages: Messages): Html =
    optOpenObligation match {
      case Some(o) if o.isOverdue && isSubscribed =>
        Html(
          messages(
            "account.overdue.return.subHeading",
            ViewUtils.formatLocalDate(o.inboundCorrespondenceFromDate),
            ViewUtils.formatLocalDate(o.inboundCorrespondenceToDate)
          )
        )
      case Some(o) if isSubscribed                =>
        Html(
          messages(
            "account.due.return.subHeading",
            ViewUtils.formatLocalDate(o.inboundCorrespondenceFromDate),
            ViewUtils.formatLocalDate(o.inboundCorrespondenceToDate),
            ViewUtils.formatLocalDate(o.inboundCorrespondenceDueDate)
          )
        )
      case _                                      => Html(messages("account.noneDue.return.subHeading"))
    }

}
