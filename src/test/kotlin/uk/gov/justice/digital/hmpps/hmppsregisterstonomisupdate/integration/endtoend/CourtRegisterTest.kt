package uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.integration.endtoend

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.helpers.courtRegisterUpdateMessage
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.wiremock.CourtRegisterApiExtension.Companion.courtRegisterApi
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.wiremock.PrisonApiExtension.Companion.prisonApi

class CourtRegisterTest : SqsIntegrationTestBase() {
  @Test
  fun `will consume a COURT_REGISTER_UPDATE message`() {

    prisonApi.stubAgencyGet("SHFCC")
    prisonApi.stubRefLookup("ADDR_TYPE", "BUS", "Business Address")
    prisonApi.stubRefLookup("CITY", "243234", "Sheffield")
    prisonApi.stubRefLookup("COUNTY", "SOUTH_YORKS", "S.Yorkshire")
    prisonApi.stubRefLookup("COUNTRY", "ENG", "England")

    courtRegisterApi.stubCourtGet("SHFCC")

    val message = courtRegisterUpdateMessage()

    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }

    awsSqsClient.sendMessage(queueUrl, message)

    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }
  }

  private fun getNumberOfMessagesCurrentlyOnQueue(): Int? {
    val queueAttributes = awsSqsClient.getQueueAttributes(queueUrl, listOf("ApproximateNumberOfMessages"))
    return queueAttributes.attributes["ApproximateNumberOfMessages"]?.toInt()
  }
}
