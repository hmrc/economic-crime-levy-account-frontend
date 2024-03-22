package uk.gov.hmrc.economiccrimelevyaccount

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.Eventually.eventually
import uk.gov.hmrc.economiccrimelevyaccount.controllers.routes
import uk.gov.hmrc.economiccrimelevyaccount.base.ISpecBase
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyaccount.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyaccount.utils.HttpHeader

class ViewYourPaymentsISpec extends ISpecBase with AuthorisedBehaviour {

  private val expectedCallsOnRetry: Int = 4

  s"GET ${routes.ViewYourPaymentsController.onPageLoad().url}" should {
    behave like authorisedActionRoute(routes.ViewYourPaymentsController.onPageLoad())

    "respond with 200 status and return correct view" in {
      stubAuthorised()
      stubFinancialData()
      stubGetSubscriptionStatus(testEclReference, testSubscribedSubscriptionStatus)

      val result = callRoute(FakeRequest(routes.ViewYourPaymentsController.onPageLoad()))

      status(result) shouldBe OK
      html(result)     should include("Your Economic Crime Levy payments")

      verify(
        1,
        getRequestedFor(urlEqualTo(s"/economic-crime-levy-account/financial-data"))
          .withHeader(HttpHeader.CorrelationId, matching(uuidRegex))
      )

      verify(
        1,
        getRequestedFor(
          urlEqualTo(s"/economic-crime-levy-registration/subscription-status/ZECL/${testEclReference.value}")
        )
          .withHeader(HttpHeader.CorrelationId, matching(uuidRegex))
      )
    }
    "retry the get submission call 3 times after the initial attempt if it fails with a 500 INTERNAL_SERVER_ERROR response" in {
      stubAuthorised()
      stubFinancialDataError()

      val result = callRoute(FakeRequest(routes.ViewYourPaymentsController.onPageLoad()))

      status(result) shouldBe INTERNAL_SERVER_ERROR

      eventually {
        verify(
          expectedCallsOnRetry,
          getRequestedFor(urlEqualTo(s"/economic-crime-levy-account/financial-data"))
            .withHeader(HttpHeader.CorrelationId, matching(uuidRegex))
        )
      }
    }
  }

}
