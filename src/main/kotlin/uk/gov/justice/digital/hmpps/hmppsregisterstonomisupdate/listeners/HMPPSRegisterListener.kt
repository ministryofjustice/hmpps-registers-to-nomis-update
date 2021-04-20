package uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.listeners

import com.google.gson.Gson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.jms.annotation.JmsListener
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.listeners.HMPPSRegisterListener.EventType.COURT_REGISTER_INSERT
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.listeners.HMPPSRegisterListener.EventType.COURT_REGISTER_UPDATE
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.model.CourtUpdate
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.services.CourtRegisterUpdateService

@Service
class HMPPSRegisterListener(
  private val courtRegisterUpdateService: CourtRegisterUpdateService,
  private val gson: Gson
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  @JmsListener(destination = "\${sqs.queue.name}")
  fun onRegisterChange(message: String) {
    val sqsMessage: SQSMessage = gson.fromJson(message, SQSMessage::class.java)
    log.info("Received message ${sqsMessage.MessageId}")
    val changeEvent: RegisterChangeEvent = gson.fromJson(sqsMessage.Message, RegisterChangeEvent::class.java)
    when (changeEvent.eventType) {
      COURT_REGISTER_UPDATE, COURT_REGISTER_INSERT -> courtRegisterUpdateService.updateCourtDetails(CourtUpdate(courtId = changeEvent.id))
      else -> log.info("Received a message I wasn't expected $changeEvent")
    }
  }

  data class RegisterChangeEvent(
    val eventType: EventType,
    val id: String
  )

  data class SQSMessage(val Message: String, val MessageId: String)

  enum class EventType {
    COURT_REGISTER_UPDATE, COURT_REGISTER_INSERT
  }
}
