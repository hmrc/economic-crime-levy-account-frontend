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
import uk.gov.hmrc.economiccrimelevyaccount.models.audit._
import uk.gov.hmrc.economiccrimelevyaccount.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyaccount.models.{DocumentDetails, FinancialDataResponse, ObligationData, ObligationDetails, Open, OpsData}
import uk.gov.hmrc.economiccrimelevyaccount.services.{EnrolmentStoreProxyService, FinancialDataService, OpsService}
import uk.gov.hmrc.economiccrimelevyaccount.views.ViewUtils
import uk.gov.hmrc.economiccrimelevyaccount.views.html.AccountView
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.audit.http.connector.AuditConnector
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.time.LocalDate
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentsController @Inject()(
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  financialDataService: FinancialDataService,
  opsService: OpsService
)(implicit ec: ExecutionContext, hc: HeaderCarrier)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = authorise.async { implicit request =>
    getFinancialDetails().map {
      case Some(opsData) =>
        opsService.get(
          opsData.chargeReference,
          opsData.amount,
          opsData.dueDate
        )
      case _ => Future.successful(Redirect(routes.NotableErrorController.notRegistered()))
    }
  }

  private def getFinancialDetails(): Future[Option[OpsData]] =
    financialDataService.retrieveFinancialData.map {
      case Right(response) => response.documentDetails match {
        case Some(details) if details.size == 1 => financialDataService.getLatestFinancialObligation(response) match {
          case Some(data) => Some(OpsData(
            financialDataService.extractValue(details.head.chargeReferenceNumber),
            data.amount,
            Some(data.dueDate)
          ))
          case _ => None
        }
        case _ => None
      }
      case _ => None
    }
}
