package uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class CourtRegisterApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val courtRegisterApi = CourtRegisterApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    courtRegisterApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    courtRegisterApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    courtRegisterApi.stop()
  }
}

class CourtRegisterApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8082
  }

  private val COURTRESPONSE = """
    {
      "courtId": "SHFCC",
      "courtName": "Another Sheffield Crown Court",
      "type": {
        "courtType": "CRN",
        "courtName": "Crown Court"
      },
      "active": true,
      "buildings": [
        {
          "id": 898,
          "courtId": "SHFCC",
          "buildingName": "The fairway",
          "street": "121 Snaith Road",
          "town": "Sheffield",
          "county": "S.Yorkshire",
          "postcode": "S1 2RT",
          "country": "England",
          "contacts": [
            {
              "id": 1064,
              "courtId": "SHFCC",
              "buildingId": 898,
              "type": "TEL",
              "detail": "0114 24565432"
            }
          ]
        }
      ]
    }
  }
  """.trimIndent()

  fun stubHealthPing(status: Int) {
    stubFor(
      get("/health/ping").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(if (status == 200) "pong" else "some error")
          .withStatus(status)
      )
    )
  }

  fun stubCourtGet(courtId: String, response: String = COURTRESPONSE) {
    stubFor(
      get("/courts/id/$courtId").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(response)
          .withStatus(200)
      )
    )
  }
}
