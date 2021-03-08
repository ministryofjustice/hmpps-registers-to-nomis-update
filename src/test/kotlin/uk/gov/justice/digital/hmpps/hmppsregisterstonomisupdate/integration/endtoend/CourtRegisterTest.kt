package uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.integration.endtoend

import com.amazonaws.services.sqs.AmazonSQS
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.helpers.courtRegisterUpdateMessage

@SpringBootTest
@ActiveProfiles("test")
class CourtRegisterTest {
  @Qualifier("awsSqsClient")
  @Autowired
  internal lateinit var awsSqsClient: AmazonSQS

  @Value("\${sqs.queue.name}")
  lateinit var queueName: String

  @Test
  fun `will consume a COURT_REGISTER_UPDATE message`() {
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
