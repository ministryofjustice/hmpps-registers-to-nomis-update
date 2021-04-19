package uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.integration

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.CourtRegisterServer
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.OAuthMockServer
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.PrisonMockServer

@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTestBase {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient


  companion object {
    val prisonMockServer = PrisonMockServer()
    internal val oauthMockServer = OAuthMockServer()
    val courtRegisterServer = CourtRegisterServer()

    @Suppress("unused")
    @BeforeAll
    @JvmStatic
    fun startMocks() {
      prisonMockServer.start()
      oauthMockServer.start()
      courtRegisterServer.start()
    }

    @Suppress("unused")
    @AfterAll
    @JvmStatic
    fun stopMocks() {
      prisonMockServer.stop()
      oauthMockServer.stop()
      courtRegisterServer.stop()
    }
  }

  @BeforeEach
  fun resetStubs() {
    oauthMockServer.resetAll()
    prisonMockServer.resetAll()
    courtRegisterServer.resetAll()

    oauthMockServer.stubGrantToken()
  }
}
