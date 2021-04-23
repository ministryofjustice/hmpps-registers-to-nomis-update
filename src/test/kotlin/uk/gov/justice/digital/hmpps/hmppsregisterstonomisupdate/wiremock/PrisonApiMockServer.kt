package uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

class PrisonApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val prisonApi = PrisonApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    prisonApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    prisonApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    prisonApi.stop()
  }
}

class PrisonApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 9081
  }

  private val AGENCYRESPONSE = """
    {
      "agencyId": "SHFCC",
      "agencyType": "CRT",
      "description": "Sheffield Crown Court",
      "longDescription": "Sheffield Crown Court",
      "active": true,
      "addresses": [
        {
          "addressId": 543524,
          "addressType": "Business Address",
          "comment": "This is a comment text",
          "country": "England",
          "county": "S.Yorkshire",
          "flat": "3B",
          "premise": "Sheffield Court",
          "locality": "Brincliffe",
          "street": "Slinn Street",
          "town": "Sheffield",
          "postalCode": "LI1 5TH",
          "primary": true,
          "noFixedAddress": false,
          "startDate": "2005-05-12",
          "phones": [
            {
              "phoneId": 2234232,
              "number": "0114 2345678",
              "type": "TEL"
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

  fun stubAgencyGet(agencyId: String, response: String = AGENCYRESPONSE) {
    stubFor(
      get("/api/agencies/$agencyId").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(response)
          .withStatus(200)
      )
    )
  }

  fun stubRefLookup(domain: String, code: String, description: String) {
    stubFor(
      get("/api/reference-domains/domains/$domain/reverse-lookup").willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(
            "[\n" +
              "      {\n" +
              "        \"code\": \"$code\",\n" +
              "        \"description\": \"$description\",\n" +
              "        \"domain\": \"$domain\",\n" +
              "        \"activeFlag\": \"Y\"\n" +
              "      }\n" +
              "    ]"
          )
          .withStatus(200)
      )
    )
  }
}
