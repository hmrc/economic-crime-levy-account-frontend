package uk.gov.hmrc.economiccrimelevyaccount.models

import java.time.LocalDate

case class OpsData(
  chargeReference: String,
  amount: BigDecimal,
  dueDate: Option[LocalDate]
)
