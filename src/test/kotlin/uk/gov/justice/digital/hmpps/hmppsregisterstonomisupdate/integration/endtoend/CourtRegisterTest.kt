package uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.integration.endtoend

import com.amazonaws.services.sqs.AmazonSQS
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.helpers.courtRegisterUpdateMessage
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.wiremock.CourtRegisterApiExtension
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.wiremock.PrisonApiExtension

@ExtendWith(PrisonApiExtension::class, CourtRegisterApiExtension::class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles(profiles = ["test"])
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
class CourtRegisterTest {
  @Qualifier("awsSqsClient")
  @Autowired
  internal lateinit var awsSqsClient: AmazonSQS

  @Value("\${sqs.queue.name}")
  lateinit var queueName: String

  @Test
  fun `will consume a COURT_REGISTER_UPDATE message`() {

    PrisonApiExtension.prisonApi.stubAgencyGet("SHFCC")
    PrisonApiExtension.prisonApi.stubRefLookup("ADDR_TYPE", "BUS", "Business Address")
    PrisonApiExtension.prisonApi.stubRefLookup("CITY", "243234", "Sheffield")
    PrisonApiExtension.prisonApi.stubRefLookup("COUNTY", "SOUTH_YORKS", "S.Yorkshire")
    PrisonApiExtension.prisonApi.stubRefLookup("COUNTRY", "ENG", "England")

    CourtRegisterApiExtension.courtRegisterApi.stubCourtGet("SHFCC")

    val message = courtRegisterUpdateMessage()

    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }

    awsSqsClient.sendMessage(queueName.queueUrl(), message)

    await untilCallTo { getNumberOfMessagesCurrentlyOnQueue() } matches { it == 0 }
  }

  fun getNumberOfMessagesCurrentlyOnQueue(): Int? {
    val queueAttributes = awsSqsClient.getQueueAttributes(queueName.queueUrl(), listOf("ApproximateNumberOfMessages"))
    return queueAttributes.attributes["ApproximateNumberOfMessages"]?.toInt()
  }

  fun String.queueUrl(): String = awsSqsClient.getQueueUrl(this).queueUrl
}
