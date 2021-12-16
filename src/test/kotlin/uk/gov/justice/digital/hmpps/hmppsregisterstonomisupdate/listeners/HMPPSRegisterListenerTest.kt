package uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.listeners

import com.google.gson.Gson
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.helpers.courtRegisterInsertMessage
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.helpers.courtRegisterUpdateMessage
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.model.CourtUpdate
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.services.CourtRegisterUpdateService

internal class HMPPSRegisterListenerTest {
  private val courtRegisterUpdateService: CourtRegisterUpdateService = mock()
  private val gson: Gson = Gson()
  private val listener: HMPPSRegisterListener =
    HMPPSRegisterListener(courtRegisterUpdateService = courtRegisterUpdateService, gson = gson)

  @Test
  internal fun `will call service for a court update`() {
    listener.onRegisterChange(courtRegisterUpdateMessage())

    verify(courtRegisterUpdateService).updateCourtDetails(CourtUpdate("SHFCC"))
  }

  @Test
  internal fun `will not call service for events we don't understand`() {
    listener.onRegisterChange(courtRegisterInsertMessage())

    verifyNoMoreInteractions(courtRegisterUpdateService)
  }
}
