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

import play.api.i18n.I18nSupport
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents, Result}
import uk.gov.hmrc.economiccrimelevyaccount.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyaccount.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyaccount.models.{FinancialData, ObligationData, ObligationDetails, Open}
import uk.gov.hmrc.economiccrimelevyaccount.services.{AuditService, ECLAccountService, EnrolmentStoreProxyService}
import uk.gov.hmrc.economiccrimelevyaccount.views.ViewUtils
import uk.gov.hmrc.economiccrimelevyaccount.views.html.AccountView
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AccountController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  enrolmentStoreProxyService: EnrolmentStoreProxyService,
  view: AccountView,
  eclAccountService: ECLAccountService,
  auditService: AuditService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with BaseController
    with ErrorHandler {

  def onPageLoad: Action[AnyContent] = authorise.async { implicit request =>
    (for {
      registrationDate             <-
        enrolmentStoreProxyService.getEclRegistrationDate(request.eclReference).asResponseError
      obligationDataOption         <- eclAccountService.retrieveObligationData.asResponseError
      latestObligationDetailsOption = obligationDataOption.flatMap(getLatestObligation)
      financialDataOption          <- eclAccountService.retrieveFinancialData.asResponseError
      response                      =
        determineResponse(registrationDate, latestObligationDetailsOption, financialDataOption)
      _                             =
        auditService
          .auditAccountViewed(request.internalId, request.eclReference, obligationDataOption, financialDataOption)
    } yield response).fold(
      presentationError => Status(presentationError.code.statusCode)(Json.toJson(presentationError)),
      result => result
    )
  }

  private def determineResponse(
    registrationDate: LocalDate,
    latestObligationDetailsOption: Option[ObligationDetails],
    financialDataOption: Option[FinancialData]
  )(implicit request: AuthorisedRequest[_]): Result =
    financialDataOption
      .map(financialData =>
        Ok(
          view(
            request.eclReference.value,
            ViewUtils.formatLocalDate(registrationDate),
            latestObligationDetailsOption,
            financialData.documentDetails match {
              case Some(_) => eclAccountService.getLatestFinancialObligation(financialData)
              case None    => None
            }
          )
        )
      )
      .getOrElse(
        Ok(
          view(
            request.eclReference.value,
            ViewUtils.formatLocalDate(registrationDate),
            latestObligationDetailsOption,
            None
          )
        )
      )

  private def getLatestObligation(obligationData: ObligationData): Option[ObligationDetails] =
    obligationData.obligations
      .flatMap(
        _.obligationDetails
          .filter(_.status == Open)
          .sortBy(_.inboundCorrespondenceDueDate)
      )
      .headOption

}
