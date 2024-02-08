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

import cats.data.EitherT
import play.api.i18n.I18nSupport
import play.api.mvc.{Action, AnyContent, MessagesControllerComponents}
import uk.gov.hmrc.economiccrimelevyaccount.controllers.actions.AuthorisedAction
import uk.gov.hmrc.economiccrimelevyaccount.models.errors.OpsError
import uk.gov.hmrc.economiccrimelevyaccount.models.{DocumentDetails, FinancialData, OpsJourneyResponse}
import uk.gov.hmrc.economiccrimelevyaccount.services.{EclAccountService, OpsService}
import uk.gov.hmrc.economiccrimelevyaccount.utils.CorrelationIdHelper
import uk.gov.hmrc.economiccrimelevyaccount.views.html.ErrorTemplate
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PaymentsController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  eclAccountService: EclAccountService,
  opsService: OpsService
)(implicit ec: ExecutionContext, errorTemplate: ErrorTemplate)
    extends FrontendBaseController
    with I18nSupport
    with BaseController
    with ErrorHandler {

  def onPageLoad(chargeReference: Option[String]): Action[AnyContent] = authorise.async { implicit request =>
    implicit val hc: HeaderCarrier = CorrelationIdHelper.getOrCreateCorrelationId(request)
    (for {
      financialData      <- eclAccountService.retrieveFinancialData.asResponseError
      opsJourneyResponse <- startOpsJourneyWithFinancialData(financialData, chargeReference).asResponseError
    } yield opsJourneyResponse).fold(
      error => routeError(error),
      response => route(response)
    )
  }

  private def route(opsJourneyResponseOption: Option[OpsJourneyResponse]) =
    opsJourneyResponseOption match {
      case Some(response) => Redirect(response.nextUrl)
      case None           => Redirect(routes.AccountController.onPageLoad())
    }

  private def startOpsJourneyWithFinancialData(financialData: Option[FinancialData], chargeReference: Option[String])(
    implicit hc: HeaderCarrier
  ): EitherT[Future, OpsError, Option[OpsJourneyResponse]] =
    financialData match {
      case Some(financialData) =>
        getObligation(financialData, chargeReference) match {
          case Some(
                DocumentDetails(
                  _,
                  Some(chargeReference),
                  _,
                  _,
                  _,
                  _,
                  outstandingAmount,
                  lineItemDetails,
                  _,
                  _,
                  _,
                  _,
                  _,
                  _
                )
              ) =>
            val periodToDate = lineItemDetails.flatMap(details => details.headOption.flatMap(_.periodToDate))
            opsService.startOpsJourney(chargeReference, outstandingAmount.getOrElse(0), periodToDate).map(Option(_))
          case _ =>
            EitherT[Future, OpsError, Option[OpsJourneyResponse]](
              Future.successful(
                Left(
                  OpsError.InternalUnexpectedError(
                    "Financial data missing expected values which are required to start the OPS journey",
                    None
                  )
                )
              )
            )
        }
      case None                => EitherT[Future, OpsError, Option[OpsJourneyResponse]](Future.successful(Right(None)))
    }

  private def getObligation(
    financialData: FinancialData,
    chargeReference: Option[String]
  ): Option[DocumentDetails] =
    chargeReference match {
      case Some(chargeReference) => financialData.getFinancialObligation(chargeReference)
      case None                  => financialData.latestFinancialObligation
    }
}
