package uk.gov.hmrc.economiccrimelevyaccount

import com.github.tomakehurst.wiremock.client.WireMock._
import org.scalatest.concurrent.Eventually.eventually
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyaccount.base.ISpecBase
import uk.gov.hmrc.economiccrimelevyaccount.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyaccount.controllers.routes
import uk.gov.hmrc.economiccrimelevyaccount.models.{Fulfilled, Obligation, ObligationData, ObligationDetails}
import uk.gov.hmrc.economiccrimelevyaccount.utils.HttpHeader

import java.time.LocalDate

class ViewYourReturnsISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.ViewYourReturnsController.onPageLoad().url}" should {
    behave like authorisedActionRoute(routes.ViewYourReturnsController.onPageLoad())

    "respond with 200 status and return correct view" in {
      stubAuthorised()
      stubFinancialData()

      val date           = LocalDate.now
      val obligationData = ObligationData(
        obligations = Seq(
          Obligation(
            obligationDetails = Seq(
              ObligationDetails(
                status = Fulfilled,
                inboundCorrespondenceFromDate = date,
                inboundCorrespondenceToDate = date,
                inboundCorrespondenceDateReceived = None,
                inboundCorrespondenceDueDate = date,
                periodKey = periodKey
              )
            )
          )
        )
      )

      stubGetObligations(obligationData)
      stubGetSubscriptionStatus(testEclReference, testSubscribedSubscriptionStatus)

      val result = callRoute(FakeRequest(routes.ViewYourReturnsController.onPageLoad()))

      status(result) shouldBe OK
      html(result)     should include("Your Economic Crime Levy returns")

      verify(
        1,
        getRequestedFor(
          urlEqualTo(s"/economic-crime-levy-account/financial-data")
        )
          .withHeader(HttpHeader.CorrelationId, matching(uuidRegex))
      )
      verify(
        1,
        getRequestedFor(
          urlEqualTo(s"/economic-crime-levy-account/obligation-data")
        )
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
      stubGetObligationsError()

      val result = callRoute(FakeRequest(routes.ViewYourReturnsController.onPageLoad()))

      status(result) shouldBe INTERNAL_SERVER_ERROR

      eventually {
        verify(
          1,
          getRequestedFor(urlEqualTo(s"/economic-crime-levy-account/obligation-data"))
            .withHeader(HttpHeader.CorrelationId, matching(uuidRegex))
        )
      }
    }

    "retry get retrieveObligationData call 3 times without calling retrieveFinancialData and getSubscriptionStatus at all" in {

      stubAuthorised()
      stubGetObligationsError()
      stubFinancialData()
      stubGetSubscriptionStatus(testEclReference, testSubscribedSubscriptionStatus)

      val result = callRoute(FakeRequest(routes.ViewYourReturnsController.onPageLoad()))

      status(result) shouldBe INTERNAL_SERVER_ERROR

      eventually {
        verify(
          1,
          getRequestedFor(urlEqualTo(s"/economic-crime-levy-account/obligation-data"))
            .withHeader(HttpHeader.CorrelationId, matching(uuidRegex))
        )

        verify(
          0,
          getRequestedFor(urlEqualTo(s"/economic-crime-levy-account/financial-data"))
            .withHeader(HttpHeader.CorrelationId, matching(uuidRegex))
        )

        verify(
          0,
          getRequestedFor(
            urlEqualTo(s"/economic-crime-levy-registration/subscription-status/ZECL/${testEclReference.value}")
          )
            .withHeader(HttpHeader.CorrelationId, matching(uuidRegex))
        )
      }
    }
  }
}
