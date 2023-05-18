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
import uk.gov.hmrc.economiccrimelevyaccount.models.{ObligationData, ObligationDetails, Open}
import uk.gov.hmrc.economiccrimelevyaccount.viewmodels.ReturnsOverview

import javax.inject.{Inject, Singleton}
import uk.gov.hmrc.economiccrimelevyaccount.views.html.{NoSubmittedReturnsView, ReturnsView}
import uk.gov.hmrc.play.bootstrap.frontend.controller.FrontendBaseController

import java.time.LocalDate
import scala.concurrent.ExecutionContext

@Singleton
class ViewYourReturnsController @Inject() (
  val controllerComponents: MessagesControllerComponents,
  authorise: AuthorisedAction,
  obligationDataConnector: ObligationDataConnector,
  returnsView: ReturnsView,
  noSubmittedReturnsView: NoSubmittedReturnsView
)(implicit ec: ExecutionContext)
    extends FrontendBaseController
    with I18nSupport {

  def onPageLoad: Action[AnyContent] = authorise.async { implicit request =>
    obligationDataConnector.getObligationData().map {
      case Some(obligationData) =>
        Ok(returnsView(assembleReturnsViewData(obligationData, request.eclRegistrationReference)))
      case None                 => Ok(noSubmittedReturnsView())
    }
  }

  private def assembleReturnsViewData(obligationData: ObligationData, eclReference: String): Seq[ReturnsOverview] =
    obligationData.obligations.flatMap(_.obligationDetails.sortBy(_.inboundCorrespondenceDueDate)).map { details =>
      ReturnsOverview(
        forgeFromToCaption(details.inboundCorrespondenceFromDate.getYear, details.inboundCorrespondenceToDate.getYear),
        details.inboundCorrespondenceDueDate,
        resolveStatus(details),
        details.periodKey,
        eclReference
      )
    }

  private def resolveStatus(details: ObligationDetails): String = details.status match {
    case Open if details.inboundCorrespondenceDueDate.isBefore(LocalDate.now()) => "OVERDUE"
    case Open
        if details.inboundCorrespondenceDueDate.isAfter(LocalDate.now()) |
          details.inboundCorrespondenceDueDate.isEqual(LocalDate.now()) =>
      "DUE"
    case _                                                                      => "SUBMITTED"
  }

  private def forgeFromToCaption(yearFrom: Integer, yearTo: Integer): String = s"$yearFrom-$yearTo"
}
