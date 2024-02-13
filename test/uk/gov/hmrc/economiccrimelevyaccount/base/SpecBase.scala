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

package uk.gov.hmrc.economiccrimelevyaccount.base

import akka.actor.ActorSystem
import com.typesafe.config.Config
import org.mockito.MockitoSugar
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.{BeforeAndAfterEach, OptionValues, TryValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import play.api.Application
import play.api.i18n.{Messages, MessagesApi}
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.mvc._
import play.api.test.Helpers.{stubBodyParser, stubControllerComponents}
import play.api.test.{DefaultAwaitTimeout, FakeRequest, FutureAwaits}
import uk.gov.hmrc.economiccrimelevyaccount.EclTestData
import uk.gov.hmrc.economiccrimelevyaccount.config.AppConfig
import uk.gov.hmrc.economiccrimelevyaccount.controllers.actions.FakeAuthorisedAction
import uk.gov.hmrc.economiccrimelevyaccount.models.requests.AuthorisedRequest
import uk.gov.hmrc.economiccrimelevyaccount.views.html.ErrorTemplate
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext

trait SpecBase
    extends AnyWordSpec
    with Matchers
    with TryValues
    with OptionValues
    with DefaultAwaitTimeout
    with FutureAwaits
    with Results
    with GuiceOneAppPerSuite
    with MockitoSugar
    with BeforeAndAfterEach
    with ScalaCheckPropertyChecks
    with EclTestData {

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type]                   = FakeRequest()
  val requestWithEclReference: AuthorisedRequest[AnyContentAsEmpty.type] = AuthorisedRequest(
    FakeRequest(),
    testInternalId,
    testEclReference
  )
  val messagesApi: MessagesApi                                           = app.injector.instanceOf[MessagesApi]
  val messages: Messages                                                 = messagesApi.preferred(fakeRequest)
  val bodyParsers: PlayBodyParsers                                       = app.injector.instanceOf[PlayBodyParsers]
  val actorSystem: ActorSystem                                           = ActorSystem("test")
  val config: Config                                                     = app.injector.instanceOf[Config]
  lazy val appConfig: AppConfig                                          = app.injector.instanceOf[AppConfig]

  implicit val errorTemplate: ErrorTemplate = app.injector.instanceOf[ErrorTemplate]

  def moduleOverrides(): Seq[GuiceableModule] = Seq.empty

  override def fakeApplication(): Application =
    new GuiceApplicationBuilder()
      .overrides(moduleOverrides(): _*)
      .configure(
        "metrics.jvm"                  -> false,
        "metrics.enabled"              -> false,
        "http-verbs.retries.intervals" -> List("1ms")
      )
      .build()

  def fakeAuthorisedAction = new FakeAuthorisedAction(bodyParsers)

  val mcc: DefaultMessagesControllerComponents = {
    val stub = stubControllerComponents()
    DefaultMessagesControllerComponents(
      new DefaultMessagesActionBuilderImpl(stubBodyParser(AnyContentAsEmpty), stub.messagesApi)(stub.executionContext),
      DefaultActionBuilder(stub.actionBuilder.parser)(stub.executionContext),
      stub.parsers,
      messagesApi,
      stub.langs,
      stub.fileMimeTypes,
      stub.executionContext
    )
  }

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()

}
