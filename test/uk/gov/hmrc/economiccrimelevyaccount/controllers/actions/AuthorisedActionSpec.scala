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

package uk.gov.hmrc.economiccrimelevyaccount.controllers.actions

import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.any
import play.api.mvc.{BodyParsers, Request, Result}
import play.api.test.Helpers._
import uk.gov.hmrc.auth.core._
import uk.gov.hmrc.auth.core.retrieve.v2.Retrievals
import uk.gov.hmrc.auth.core.retrieve.{Retrieval, ~}
import uk.gov.hmrc.auth.core.syntax.retrieved.authSyntaxForRetrieved
import uk.gov.hmrc.economiccrimelevyaccount.base.SpecBase
import uk.gov.hmrc.economiccrimelevyaccount.controllers.routes
import uk.gov.hmrc.economiccrimelevyaccount.models.eacd.EclEnrolment
import uk.gov.hmrc.economiccrimelevyaccount.{EnrolmentsWithEcl, EnrolmentsWithoutEcl}

import scala.concurrent.Future

class AuthorisedActionSpec extends SpecBase {

  val defaultBodyParser: BodyParsers.Default = app.injector.instanceOf[BodyParsers.Default]
  val mockAuthConnector: AuthConnector       = mock[AuthConnector]

  val authorisedAction =
    new BaseAuthorisedAction(mockAuthConnector, appConfig, defaultBodyParser)

  val testAction: Request[_] => Future[Result] = { _ =>
    Future(Ok("Test"))
  }

  val expectedRetrievals: Retrieval[Option[String] ~ Enrolments] =
    Retrievals.internalId and Retrievals.authorisedEnrolments

  "invokeBlock" should {
    "execute the block and return the result if authorised" in forAll {
      (internalId: String, enrolmentsWithEcl: EnrolmentsWithEcl) =>
        when(mockAuthConnector.authorise(any(), ArgumentMatchers.eq(expectedRetrievals))(any(), any()))
          .thenReturn(Future(Some(internalId) and enrolmentsWithEcl.enrolments))

        val result: Future[Result] = authorisedAction.invokeBlock(fakeRequest, testAction)

        status(result)          shouldBe OK
        contentAsString(result) shouldBe "Test"
    }

    "redirect the user to sign in when there is no active session" in {
      List(BearerTokenExpired(), MissingBearerToken(), InvalidBearerToken(), SessionRecordNotFound()).foreach {
        exception =>
          when(mockAuthConnector.authorise[Unit](any(), any())(any(), any())).thenReturn(Future.failed(exception))

          val result: Future[Result] = authorisedAction.invokeBlock(fakeRequest, testAction)

          status(result)               shouldBe SEE_OTHER
          redirectLocation(result).value should startWith(appConfig.signInUrl)
      }
    }

    "redirect the user to the not enrolled page if they have insufficient enrolments" in {
      when(
        mockAuthConnector
          .authorise(any(), ArgumentMatchers.eq(expectedRetrievals))(any(), any())
      )
        .thenReturn(Future.failed(InsufficientEnrolments()))

      val result: Future[Result] = authorisedAction.invokeBlock(fakeRequest, testAction)

      status(result)                 shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe routes.NotableErrorController.notRegistered().url
    }

    "redirect the user to the agent not supported page if they have an unsupported affinity group" in {
      when(
        mockAuthConnector
          .authorise(any(), ArgumentMatchers.eq(expectedRetrievals))(any(), any())
      )
        .thenReturn(
          Future.failed(UnsupportedAffinityGroup())
        )

      val result: Future[Result] = authorisedAction.invokeBlock(fakeRequest, testAction)

      status(result)                 shouldBe SEE_OTHER
      redirectLocation(result).value shouldBe routes.NotableErrorController
        .agentCannotAccessService()
        .url
    }

    "throw an IllegalStateException if there is no internal id" in {
      when(mockAuthConnector.authorise(any(), ArgumentMatchers.eq(expectedRetrievals))(any(), any()))
        .thenReturn(Future(None and Enrolments(Set.empty)))

      val result = intercept[IllegalStateException] {
        await(authorisedAction.invokeBlock(fakeRequest, testAction))
      }

      result.getMessage shouldBe "Unable to retrieve internalId"
    }

    "throw an IllegalStateException if there is no ECL enrolment in the set of authorised enrolments" in forAll {
      (internalId: String, enrolmentsWithoutEcl: EnrolmentsWithoutEcl) =>
        when(mockAuthConnector.authorise(any(), ArgumentMatchers.eq(expectedRetrievals))(any(), any()))
          .thenReturn(Future(Some(internalId) and enrolmentsWithoutEcl.enrolments))

        val result = intercept[IllegalStateException] {
          await(authorisedAction.invokeBlock(fakeRequest, testAction))
        }

        result.getMessage shouldBe s"Unable to retrieve enrolment with key ${EclEnrolment.serviceName} and identifier ${EclEnrolment.identifierKey}"
    }
  }

}
