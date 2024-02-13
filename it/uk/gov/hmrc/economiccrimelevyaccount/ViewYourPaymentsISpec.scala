package uk.gov.hmrc.economiccrimelevyaccount

import uk.gov.hmrc.economiccrimelevyaccount.controllers.routes
import uk.gov.hmrc.economiccrimelevyaccount.base.ISpecBase
import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyaccount.behaviours.AuthorisedBehaviour

class ViewYourPaymentsISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.ViewYourPaymentsController.onPageLoad().url}" should {
    behave like authorisedActionRoute(routes.ViewYourPaymentsController.onPageLoad())

    "respond with 200 status and return correct view" in {
      stubAuthorised()
      stubFinancialData()
      stubGetSubscriptionStatus(testEclReference, testSubscribedSubscriptionStatus)

      val result = callRoute(FakeRequest(routes.ViewYourPaymentsController.onPageLoad()))

      status(result) shouldBe OK
      html(result)     should include("Your Economic Crime Levy payments")
    }
  }

}
