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
  private val prisonReferenceDataService: PrisonReferenceDataService = mock()
  private val prisonService: PrisonService = mock()
  private val telemetryClient: TelemetryClient = mock()
  private lateinit var service: CourtRegisterSyncService

  @BeforeEach
  fun before() {
    val courtRegisterUpdateService =
      CourtRegisterUpdateService(courtRegisterService, prisonService, prisonReferenceDataService, telemetryClient, false, GsonConfig().gson())

    service = CourtRegisterSyncService(courtRegisterUpdateService, prisonReferenceDataService, courtRegisterService, prisonService)

    whenever(prisonReferenceDataService.getRefCode(eq("ADDR_TYPE"), eq("Business Address"), eq(false))).thenReturn(
      ReferenceCode("ADDR_TYPE", "BUS", "Business Address", "Y", null)
    )

    whenever(prisonReferenceDataService.getRefCode(eq("CITY"), eq("Sheffield"), eq(false))).thenReturn(
      ReferenceCode("CITY", "892734", "Sheffield", "Y", null)
    )

    whenever(prisonReferenceDataService.getRefCode(eq("COUNTY"), eq("South Yorkshire"), eq(false))).thenReturn(
      ReferenceCode("COUNTY", "S.YORKSHIRE", "South Yorkshire", "Y", null)
    )

    whenever(prisonReferenceDataService.getRefCode(eq("COUNTRY"), eq("England"), eq(false))).thenReturn(
      ReferenceCode("COUNTRY", "ENG", "England", "Y", null)
    )
  }

  @Test
  fun `should perform updates`() {
    val courtRegisterData = listOf(
      generateCourtRegisterEntry("SHFCC", "Sheffield Crown Court"),
      generateCourtRegisterEntry("SHFC1", "Sheffield Crown Court 1"),
      generateCourtRegisterEntry("SHFC2", "Sheffield Crown Court 2")
    )

    whenever(courtRegisterService.getAllActiveCourts()).thenReturn(courtRegisterData)

    whenever(prisonService.getAllCourts()).thenReturn(
      listOf(
        courtFromPrisonSystem("SHFCC", "Sheffield Crown Court"),
        courtFromPrisonSystem("SHFC3", "Sheffield Crown Court 3")
      )
    )
    val stats = service.sync()
    val diffs = stats.diffs
    assertThat(diffs).hasSize(4)
    assertThat(diffs[0].areEqual()).isTrue
    assertThat(diffs[1].areEqual()).isFalse
    assertThat(diffs[1].entriesOnlyOnRight().get("courtId")).isEqualTo("SHFC1")
    assertThat(diffs[2].areEqual()).isFalse
    assertThat(diffs[2].entriesOnlyOnRight().get("courtId")).isEqualTo("SHFC2")
    assertThat(diffs[3].areEqual()).isFalse
    assertThat(diffs[3].entriesInCommon().get("courtId")).isEqualTo("SHFC3")
    assertThat(diffs[3].entriesDiffering().get("active")?.leftValue()).isEqualTo(true)
    assertThat(diffs[3].entriesDiffering().get("active")?.rightValue()).isEqualTo(false)
  }

  private fun courtFromPrisonSystem(courtId: String, name: String) = CourtFromPrisonSystem(
    courtId, name,
    "Sheffield Crown Court in Sheffield", "CRT", true, "CC", null,
    listOf(
      addressFromPrisonSystem()
    )
  )

  private fun generateCourtRegisterEntry(courtId: String, name: String) = CourtDto(
    courtId,
    name,
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
