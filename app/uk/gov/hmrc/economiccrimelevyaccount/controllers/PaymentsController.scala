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
import play.api.mvc.Results.Redirect
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyaccount.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyaccount.models.OpsData
import uk.gov.hmrc.economiccrimelevyaccount.services.{FinancialDataService, OpsService}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentsController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  financialDataService: FinancialDataService,
  opsService: OpsService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = authorise.async { implicit request =>
    getFinancialDetails.flatMap {
      case Some(value) =>
        opsService.startOpsJourney(value.chargeReference, value.amount, value.dueDate).map {
          case Right(r) => Redirect(r.nextUrl)
          case Left(_)  => Redirect(routes.AccountController.onPageLoad())
        }
      case None        => Future.successful(Redirect(routes.AccountController.onPageLoad()))
    }
  }

  private def getFinancialDetails()(implicit
    hc: HeaderCarrier
  ): Future[Option[OpsData]] =
    financialDataService.retrieveFinancialData.flatMap {
      case Left(_)         => Future.successful(None)
      case Right(response) =>
        financialDataService.getLatestFinancialObligation(response).map {
          case Some(value) =>
            Some(
              OpsData(
                value.chargeReference,
                value.amount - value.paidAmount,
                Some(value.dueDate)
              )
            )
          case None        => None
        }
    }
}
