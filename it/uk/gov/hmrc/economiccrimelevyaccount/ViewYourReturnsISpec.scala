package uk.gov.hmrc.economiccrimelevyaccount

import play.api.test.FakeRequest
import uk.gov.hmrc.economiccrimelevyaccount.base.ISpecBase
import com.danielasfregola.randomdatagenerator.RandomDataGenerator.random
import uk.gov.hmrc.economiccrimelevyaccount.behaviours.AuthorisedBehaviour
import uk.gov.hmrc.economiccrimelevyaccount.generators.CachedArbitraries._
import uk.gov.hmrc.economiccrimelevyaccount.controllers.routes
import uk.gov.hmrc.economiccrimelevyaccount.models.{Obligation, ObligationData, ObligationDetails, Open}

import java.time.LocalDate

class ViewYourReturnsISpec extends ISpecBase with AuthorisedBehaviour {

  s"GET ${routes.ViewYourReturnsController.onPageLoad().url}" should {
    behave like authorisedActionRoute(routes.ViewYourReturnsController.onPageLoad())

    "respond with 200 status and return correct view" in {
      stubAuthorised()
      stubFinancialData
      val now            = LocalDate.now
      val obligationData = ObligationData(
        obligations = Seq(
          Obligation(
            obligationDetails = Seq(
              ObligationDetails(
                status = Open,
                inboundCorrespondenceFromDate = now,
                inboundCorrespondenceToDate = now,
                inboundCorrespondenceDateReceived = None,
                inboundCorrespondenceDueDate = now,
                periodKey = "21XX"
              )
            )
          )
        )
      )

      stubGetObligations(obligationData)

      val result = callRoute(FakeRequest(routes.ViewYourReturnsController.onPageLoad()))

      status(result) shouldBe OK
      html(result)     should include("Your Economic Crime Levy returns")
    }
  }
}
