package uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.listeners

import com.google.gson.Gson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.model.CourtUpdate
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.services.CourtRegisterUpdateService

@Service
class HMPPSRegisterListener(
  private val courtRegisterUpdateService: CourtRegisterUpdateService,
  private val gson: Gson,
) {

  private companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @JmsListener(destination = "registers", containerFactory = "hmppsQueueContainerFactoryProxy")
  fun onRegisterChange(message: String) {
    val sqsMessage: SQSMessage = gson.fromJson(message, SQSMessage::class.java)
    log.info("Received message {}", sqsMessage.MessageId)
    val changeEvent: RegisterChangeEvent = gson.fromJson(sqsMessage.Message, RegisterChangeEvent::class.java)
    when (changeEvent.eventType) {
      "COURT_REGISTER_UPDATE" -> courtRegisterUpdateService.updateCourtDetails(CourtUpdate(courtId = changeEvent.id))
      else -> log.info("Received a message I wasn't expecting {}", changeEvent)
    }
  }

  data class RegisterChangeEvent(
    val eventType: String,
    val id: String
  )

  data class SQSMessage(val Message: String, val MessageId: String)
}
