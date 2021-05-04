package uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.services

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.config.GsonConfig
import java.time.LocalDate

class CourtRegisterSyncServiceTest {

  private val courtRegisterService: CourtRegisterService = mock()
  private val prisonService: PrisonService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private lateinit var service: CourtRegisterSyncService

  @BeforeEach
  fun before() {
    val courtRegisterUpdateService =
      CourtRegisterUpdateService(courtRegisterService, prisonService, telemetryClient, false, GsonConfig().gson())

    service = CourtRegisterSyncService(courtRegisterUpdateService, courtRegisterService, prisonService)

    whenever(prisonService.lookupCodeForReferenceDescriptions(eq("ADDR_TYPE"), eq("Business Address"), eq(false))).thenReturn(
      listOf(ReferenceCode("ADDR_TYPE", "BUS", "Business Address", "Y", null))
    )

    whenever(prisonService.lookupCodeForReferenceDescriptions(eq("CITY"), eq("Sheffield"), eq(false))).thenReturn(
      listOf(ReferenceCode("CITY", "892734", "Sheffield", "Y", null))
    )

    whenever(prisonService.lookupCodeForReferenceDescriptions(eq("COUNTY"), eq("South Yorkshire"), eq(false))).thenReturn(
      listOf(ReferenceCode("COUNTY", "S.YORKSHIRE", "South Yorkshire", "Y", null))
    )

    whenever(prisonService.lookupCodeForReferenceDescriptions(eq("COUNTRY"), eq("England"), eq(false))).thenReturn(
      listOf(ReferenceCode("COUNTRY", "ENG", "England", "Y", null))
    )
  }

  @Test
  fun `should perform no update`() {
    val courtRegisterData = generateCourtRegisterEntry()

    whenever(courtRegisterService.getAllActiveCourts()).thenReturn(listOf(courtRegisterData))

    whenever(prisonService.getAllCourts()).thenReturn(
      listOf(
        CourtFromPrisonSystem(
          "SHFCC", "Sheffield Crown Court",
          "Sheffield Crown Court in Sheffield", "CRT", true, "CC", null,
          listOf(
            addressFromPrisonSystem()
          )
        )
      )
    )
    val diffs = service.sync()
    assertThat(diffs).hasSize(1)
    assertThat(diffs[0].areEqual()).isTrue
  }

  private fun generateCourtRegisterEntry() = CourtDto(
    "SHFCC",
    "Sheffield Crown Court",
    "Sheffield Crown Court in Sheffield",
    CourtTypeDto("CRN", "Crown Court"),
    true,
    listOf(
      BuildingDto(
        1L,
        "SHFCC",
        null,
        "Main Sheffield Court Building",
        "Law Street",
        "Kelham Island",
        "Sheffield",
        "South Yorkshire",
        "S1 5TT",
        "England",
        listOf(
          ContactDto(1L, "SHFCC", 1L, "TEL", "0114 1232311"),
          ContactDto(2L, "SHFCC", 1L, "FAX", "0114 1232312")
        )
      )
    )
  )

  private fun addressFromPrisonSystem() =
    AddressFromPrisonSystem(
      56L, "Business Address", null, "Main Sheffield Court Building", "Law Street", "Kelham Island", "Sheffield",
      "S1 5TT", "South Yorkshire", "England", true, false, LocalDate.now(), null,
      phoneList()
    )

  private fun phoneList() =
    listOf(
      PhoneFromPrisonSystem(23432L, "0114 1232311", "BUS", null),
      PhoneFromPrisonSystem(23437L, "0114 1232312", "FAX", null)
    )
}
