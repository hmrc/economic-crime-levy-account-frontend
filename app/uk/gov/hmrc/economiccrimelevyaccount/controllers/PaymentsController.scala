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
import uk.gov.hmrc.economiccrimelevyaccount.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyaccount.models.OpsData
import uk.gov.hmrc.economiccrimelevyaccount.services.{ECLAccountService, OpsService}
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.FinancialViewDetails
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentsController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  eclAccountService: ECLAccountService,
  opsService: OpsService
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport
    with BaseController
    with ErrorHandler {

  def onPageLoad: Action[AnyContent] = authorise.async { implicit request =>
    (for {
      financialDataOption <- eclAccountService.retrieveFinancialData.asResponseError
    } yield financialDataOption).convertToResultWithJsonBody(OK)
  }

//  def getFinancialDetails(implicit hc: HeaderCarrier): Future[Option[FinancialViewDetails]] =
//    eclAccountService.getFinancialDetails.map {
//      case None           => None
//      case Some(response) =>
//        val preparedFinancialDetails = prepareFinancialDetails(response)
//        if (preparedFinancialDetails.paymentHistory.isEmpty & preparedFinancialDetails.outstandingPayments.isEmpty) {
//          None
//        } else {
//          Some(preparedFinancialDetails)
//        }
//    }

//  private def getFinancialDetails()(implicit
//    hc: HeaderCarrier
//  ): Future[Option[OpsData]] =
//    financialDataService.retrieveFinancialData.map {
//      case None           => None
//      case Some(response) =>
//        financialDataService.getLatestFinancialObligation(response) match {
//          case Some(value) =>
//            Some(
//              OpsData(
//                value.chargeReference,
//                value.amount,
//                Some(value.dueDate)
//              )
//            )
//          case None        => None
//        }
//    }
}
