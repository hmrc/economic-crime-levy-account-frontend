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
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyaccount.connectors.ObligationDataConnector
import uk.gov.hmrc.economiccrimelevyaccount.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyaccount.models.audit.AccountViewedAuditEvent
import uk.gov.hmrc.economiccrimelevyaccount.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyaccount.models.{ObligationData, ObligationDetails, Open}
import uk.gov.hmrc.economiccrimelevyaccount.services.EnrolmentStoreProxyService
import uk.gov.hmrc.economiccrimelevyaccount.views.ViewUtils
import uk.gov.hmrc.economiccrimelevyaccount.views.html.AccountView
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.ExecutionContext

@Singleton
class AccountController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  enrolmentStoreProxyService: EnrolmentStoreProxyService,
  view: AccountView,
  obligationDataConnector: ObligationDataConnector,
  auditConnector: AuditConnector
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = authorise.async { implicit request =>
    enrolmentStoreProxyService.getEclRegistrationDate(request.eclRegistrationReference).flatMap { registrationDate =>
      obligationDataConnector
        .getObligationData()
        .map { o =>
          auditAccountViewed(o)

          o match {
            case Some(obligationData) =>
              Ok(
                view(
                  request.eclRegistrationReference,
                  ViewUtils.formatLocalDate(registrationDate),
                  getLatestObligation(obligationData)
                )
              )
            case None                 =>
              Ok(view(request.eclRegistrationReference, ViewUtils.formatLocalDate(registrationDate), None))
          }
        }
    }
  }

  private def auditAccountViewed(obligationData: Option[ObligationData])(implicit request: AuthorisedRequest[_]): Unit =
    auditConnector.sendExtendedEvent(
      AccountViewedAuditEvent(
        internalId = request.internalId,
        eclReference = request.eclRegistrationReference,
        obligationDetails = obligationData.map(_.obligations.flatMap(_.obligationDetails)).toSeq.flatten
      ).extendedDataEvent
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
