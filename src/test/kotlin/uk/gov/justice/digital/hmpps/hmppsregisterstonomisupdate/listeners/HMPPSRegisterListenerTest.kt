package uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.listeners

import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.model.CourtUpdate
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.services.CourtRegisterUpdateService

internal class HMPPSRegisterListenerTest {
  private val courtRegisterUpdateService: CourtRegisterUpdateService = mock()
  private val gson: Gson = Gson()
  private val listener: HMPPSRegisterListener =
    HMPPSRegisterListener(courtRegisterUpdateService = courtRegisterUpdateService, gson = gson)

  @Test
  internal fun `will call service for a court update`() {
    val json = """
      {
        "Type": "Notification", 
        "MessageId": "48e8a79a-0f43-4338-bbd4-b0d745f1f8ec", 
        "Token": null, 
        "TopicArn": "arn:aws:sns:eu-west-2:000000000000:hmpps-domain-events", 
        "Message": "{\"eventType\":\"COURT_REGISTER_UPDATE\",\"id\":\"SHFCC\"}", 
        "SubscribeURL": null, 
        "Timestamp": "2021-03-05T11:23:56.031Z", 
        "SignatureVersion": "1", 
        "Signature": "EXAMPLEpH+..", 
        "SigningCertURL": "https://sns.us-east-1.amazonaws.com/SimpleNotificationService-0000000000000000000000.pem"}      
    """.trimIndent()

    listener.onRegisterChange(json)

    verify(courtRegisterUpdateService).updateCourtRegister(CourtUpdate("SHFCC"))
  }

  @Test
  internal fun `will not call service for events we don't understand`() {
    val json = """
      {
        "Type": "Notification", 
        "MessageId": "48e8a79a-0f43-4338-bbd4-b0d745f1f8ec", 
        "Token": null, 
        "TopicArn": "arn:aws:sns:eu-west-2:000000000000:hmpps-domain-events", 
        "Message": "{\"eventType\":\"COURT_REGISTER_INSERT\",\"id\":\"SHFCC\"}", 
        "SubscribeURL": null, 
        "Timestamp": "2021-03-05T11:23:56.031Z", 
        "SignatureVersion": "1", 
        "Signature": "EXAMPLEpH+..", 
        "SigningCertURL": "https://sns.us-east-1.amazonaws.com/SimpleNotificationService-0000000000000000000000.pem"}      
    """.trimIndent()

    listener.onRegisterChange(json)

    verifyNoMoreInteractions(courtRegisterUpdateService)
  }
}
