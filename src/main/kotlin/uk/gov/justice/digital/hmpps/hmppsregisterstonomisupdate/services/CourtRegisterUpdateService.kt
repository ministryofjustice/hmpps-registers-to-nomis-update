package uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.model.CourtUpdate

@Service
class CourtRegisterUpdateService {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun updateCourtRegister(court: CourtUpdate) {
    log.info("About to update court $court")
  }
}
