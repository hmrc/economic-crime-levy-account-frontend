package uk.gov.hmrc.economiccrimelevyaccount

import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyaccount.base.ISpecBase
import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import uk.gov.hmrc.economiccrimelevyaccount.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyaccount.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyaccount.controllers.routes
import uk.gov.hmrc.economiccrimelevyaccount.models.ObligationData

class ViewYourReturnsISpec extends ISpecBase with AuthorisedBehaviour{

  s"GET ${routes.ViewYourReturnsController.onPageLoad().url}" should{
    behave like authorisedActionRoute(routes.ViewYourReturnsController.onPageLoad())

    "respond with 200 status and return correct view" in {
      stubAuthorised()
      stubFinancialData
      val obligationData = random[ObligationData]

      stubGetObligations(obligationData)

      val result = callRoute(FakeRequest(routes.ViewYourReturnsController.onPageLoad()))

      status(result) shouldBe OK
      html(result) should include("Your Economic Crime Levy returns")
    }
  }
}
