package uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.resource

import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.boot.test.mock.mockito.MockBean
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.services.CourtDifferences
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.services.CourtRegisterSyncService
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.services.SyncStatistics

class SyncResourceTest : IntegrationTestBase() {

  @MockBean
  private lateinit var syncService: CourtRegisterSyncService

  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  @Nested
  inner class SecurePutEndpoints {
    private fun secureEndpoints() =
      listOf(
        "/sync",
      )

    @ParameterizedTest
    @MethodSource("secureEndpoints")
    internal fun `requires a valid authentication token`(uri: String) {
      webTestClient.put()
        .uri(uri)
        .exchange()
        .expectStatus().isUnauthorized
    }

    @ParameterizedTest
    @MethodSource("secureEndpoints")
    internal fun `requires the correct role`(uri: String) {
      webTestClient.put()
        .uri(uri)
        .headers(setAuthorisation(roles = listOf()))
        .exchange()
        .expectStatus().isForbidden
    }

    @ParameterizedTest
    @MethodSource("secureEndpoints")
    internal fun `satisfies the correct role but no scope`(uri: String) {
      webTestClient.put()
        .uri(uri)
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_REF_DATA")))
        .exchange()
        .expectStatus().isForbidden
    }

    @ParameterizedTest
    @MethodSource("secureEndpoints")
    internal fun `satisfies the correct role but wrong scope`(uri: String) {
      webTestClient.put()
        .uri(uri)
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_REF_DATA"), scopes = listOf("read")))
        .exchange()
        .expectStatus().isForbidden
    }

    @ParameterizedTest
    @MethodSource("secureEndpoints")
    internal fun `satisfies the correct role and scope`(uri: String) {

      val cd = CourtDifferences(
        "SHFFCC",
        "not equal: only on left={telephoneNumber=01234 645000, fax=01234 645177}",
        CourtDifferences.UpdateType.UPDATE
      )

      val mapper = mutableMapOf<String, CourtDifferences>()
      mapper["SHFFCC"] = cd
      whenever(syncService.sync()).thenReturn(
        SyncStatistics(mapper)
      )

      webTestClient.put()
        .uri(uri)
        .headers(setAuthorisation(roles = listOf("ROLE_MAINTAIN_REF_DATA"), scopes = listOf("write")))
        .exchange()
        .expectStatus().isOk
        .expectBody().json(
          """
          {
            "courts": {
              "SHFFCC": {
                "courtId":"SHFFCC",
                "differences":"not equal: only on left={telephoneNumber=01234 645000, fax=01234 645177}",
                "updateType":"UPDATE"
            }}}
          """
        )
    }
  }
}
