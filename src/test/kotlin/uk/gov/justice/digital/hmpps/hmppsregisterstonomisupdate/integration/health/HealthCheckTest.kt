package uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.integration.health

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.integration.SqsIntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.wiremock.CourtRegisterApiExtension
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.wiremock.PrisonApiExtension
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.function.Consumer

class HealthCheckTest : SqsIntegrationTestBase() {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Test
  fun `Health page reports ok`() {
    stubPingWithResponse(200)

    webTestClient.get()
      .uri("/health")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `Health info reports version`() {
    stubPingWithResponse(200)

    webTestClient.get().uri("/health")
      .exchange()
      .expectStatus().isOk
      .expectBody().jsonPath("components.healthInfo.details.version").value(
        Consumer<String> {
          assertThat(it).startsWith(LocalDateTime.now().format(DateTimeFormatter.ISO_DATE))
        }
      )
  }

  @Test
  fun `Health ping page is accessible`() {
    stubPingWithResponse(200)

    webTestClient.get()
      .uri("/health/ping")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `readiness reports ok`() {
    stubPingWithResponse(200)

    webTestClient.get()
      .uri("/health/readiness")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `liveness reports ok`() {
    stubPingWithResponse(200)

    webTestClient.get()
      .uri("/health/liveness")
      .exchange()
      .expectStatus()
      .isOk
      .expectBody()
      .jsonPath("status").isEqualTo("UP")
  }

  @Test
  fun `Queue health reports queue details`() {
    stubPingWithResponse(200)
    webTestClient.get().uri("/health")
      .exchange()
      .expectStatus().isOk
      .expectBody()
      .jsonPath("components.registers-health.status").isEqualTo("UP")
      .jsonPath("components.registers-health.details.messagesOnQueue").isEqualTo(0)
      .jsonPath("components.registers-health.details.messagesInFlight").isEqualTo(0)
      .jsonPath("components.registers-health.details.messagesOnDlq").isEqualTo(0)
      .jsonPath("components.registers-health.details.dlqStatus").isEqualTo("UP")
  }

  private fun stubPingWithResponse(status: Int) {
    HmppsAuthApiExtension.hmppsAuth.stubHealthPing(status)
    PrisonApiExtension.prisonApi.stubHealthPing(status)
    CourtRegisterApiExtension.courtRegisterApi.stubHealthPing(status)
  }
}
