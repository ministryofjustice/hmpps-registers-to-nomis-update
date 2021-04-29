package uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.services

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.config.GsonConfig
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.model.CourtUpdate
import java.time.LocalDate

class CourtRegisterUpdateServiceTest {

  private val courtRegisterService: CourtRegisterService = mock()
  private val prisonService: PrisonService = mock()
  private val telemetryClient: TelemetryClient = mock()

  private lateinit var service: CourtRegisterUpdateService

  @BeforeEach
  fun before() {
    service = CourtRegisterUpdateService(courtRegisterService, prisonService, telemetryClient, true, GsonConfig().gson())

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

    whenever(courtRegisterService.getCourtInfoFromRegister(eq("SHFCC"))).thenReturn(courtRegisterData)

    whenever(prisonService.getCourtInformation(eq("SHFCC"))).thenReturn(
      CourtFromPrisonSystem(
        "SHFCC", "Sheffield Crown Court",
        "Sheffield Crown Court in Sheffield", "CRT", true, "CC", null,
        listOf(
          addressFromPrisonSystem()
        )
      )
    )
    val diffs = service.updateCourtDetails(CourtUpdate("SHFCC"))
    assertThat(diffs).hasSize(1)
    assertThat(diffs[0].areEqual()).isTrue
  }

  @Test
  fun `should perform no update for multiple court locations`() {
    val courtRegisterData = generateCourtRegisterEntryMultipleLocations()

    whenever(courtRegisterService.getCourtInfoFromRegister(eq("SHFCC"))).thenReturn(courtRegisterData)

    whenever(prisonService.getCourtInformation(eq("SHFCC"))).thenReturn(
      CourtFromPrisonSystem(
        "SHFCC", "Sheffield Crown Court",
        "Sheffield Crown Court in Sheffield", "CRT", true, "CC", null,
        listOf(
          addressFromPrisonSystem(),
          AddressFromPrisonSystem(
            57L, "Business Address", null, "Another Sheffield Court Building - Part of the main", "Law Street", "Kelham Island", "Sheffield",
            "S1 5TT", "South Yorkshire", "England", false, false, LocalDate.now(), null,
            listOf(PhoneFromPrisonSystem(22432L, "0114 1232318", "BUS", null))
          )
        )
      )
    )

    whenever(prisonService.getCourtInformation(eq("SHFCC1"))).thenReturn(
      CourtFromPrisonSystem(
        "SHFCC1", "Sheffield Crown Court - Annex 1",
        "Sheffield Crown Court - Annex 1", "CRT", true, "CC", null,
        listOf(
          AddressFromPrisonSystem(
            50L, "Business Address", null, "Annex 1", "Law Street", "Kelham Island", "Sheffield",
            "S1 5TT", "South Yorkshire", "England", true, false, LocalDate.now(), null,
            listOf(PhoneFromPrisonSystem(95432L, "0114 1232812", "BUS", null))
          )
        )
      )
    )

    whenever(prisonService.getCourtInformation(eq("SHFCC2"))).thenReturn(
      CourtFromPrisonSystem(
        "SHFCC2", "Sheffield Crown Court - Annex 2",
        "Sheffield Crown Court - Annex 2", "CRT", true, "CC", null,
        listOf(
          AddressFromPrisonSystem(
            76L, "Business Address", null, "Annex 2", "Law Street", "Kelham Island", "Sheffield",
            "S1 5TT", "South Yorkshire", "England", true, false, LocalDate.now(), null,
            listOf(
              PhoneFromPrisonSystem(91432L, "0114 1932311", "BUS", null),
              PhoneFromPrisonSystem(91482L, "0114 1932312", "FAX", null)
            )
          )
        )
      )
    )
    val diffs = service.updateCourtDetails(CourtUpdate("SHFCC"))
    assertThat(diffs).hasSize(3)
    assertThat(diffs[0].areEqual()).isTrue
    assertThat(diffs[1].areEqual()).isTrue
    assertThat(diffs[2].areEqual()).isTrue
  }

  @Test
  fun `should perform update for multiple court locations change of description`() {
    val courtRegisterData = generateCourtRegisterEntryMultipleLocations()

    whenever(courtRegisterService.getCourtInfoFromRegister(eq("SHFCC"))).thenReturn(courtRegisterData)

    whenever(prisonService.getCourtInformation(eq("SHFCC"))).thenReturn(
      CourtFromPrisonSystem(
        "SHFCC", "Sheffield Crown Court",
        "Sheffield Crown Court in Sheffield", "CRT", true, "CC", null,
        listOf(
          addressFromPrisonSystem(),
          AddressFromPrisonSystem(
            57L, "Business Address", null, "Another Sheffield Court Building - Part of the main", "Law Street", "Kelham Island", "Sheffield",
            "S1 5TT", "South Yorkshire", "England", false, false, LocalDate.now(), null,
            listOf(PhoneFromPrisonSystem(22432L, "0114 1232318", "BUS", null))
          )
        )
      )
    )

    whenever(prisonService.getCourtInformation(eq("SHFCC1"))).thenReturn(
      CourtFromPrisonSystem(
        "SHFCC1", "Sheffield Crown Court - Annex 1",
        "Sheffield Crown Court - Annex 1 of main building", "CRT", true, "CC", null,
        listOf(
          AddressFromPrisonSystem(
            50L, "Business Address", null, "Annex 1", "Law Street", "Kelham Island", "Sheffield",
            "S1 5TT", "South Yorkshire", "England", true, false, LocalDate.now(), null,
            listOf(PhoneFromPrisonSystem(95432L, "0114 1232812", "BUS", null))
          )
        )
      )
    )

    whenever(prisonService.getCourtInformation(eq("SHFCC2"))).thenReturn(
      CourtFromPrisonSystem(
        "SHFCC2", "Sheffield Crown Court - Annex 2",
        "Sheffield Crown Court - Annex 2", "CRT", true, "CC", null,
        listOf(
          AddressFromPrisonSystem(
            76L, "Business Address", null, "Annex 2", "Law Street", "Kelham Island", "Sheffield",
            "S1 5TT", "South Yorkshire", "England", true, false, LocalDate.now(), null,
            listOf(
              PhoneFromPrisonSystem(91432L, "0114 1932311", "BUS", null),
              PhoneFromPrisonSystem(91482L, "0114 1932312", "FAX", null)
            )
          )
        )
      )
    )
    val diffs = service.updateCourtDetails(CourtUpdate("SHFCC"))
    assertThat(diffs).hasSize(3)
    assertThat(diffs[0].areEqual()).isFalse
    assertThat(diffs[0].entriesDiffering()?.size).isEqualTo(1)
    assertThat(diffs[0].entriesDiffering()?.get("longDescription")?.leftValue()).isEqualTo("Sheffield Crown Court - Annex 1 of main building")
    assertThat(diffs[0].entriesDiffering()?.get("longDescription")?.rightValue()).isEqualTo("Sheffield Crown Court - Annex 1")
    assertThat(diffs[1].areEqual()).isTrue
    assertThat(diffs[2].areEqual()).isTrue
  }

  @Test
  fun `should perform update on court only`() {
    val courtRegisterData = generateCourtRegisterEntry()

    whenever(courtRegisterService.getCourtInfoFromRegister(eq("SHFCC"))).thenReturn(courtRegisterData)

    whenever(prisonService.getCourtInformation(eq("SHFCC"))).thenReturn(
      CourtFromPrisonSystem(
        "SHFCC", "Sheffield Crown Court Wibble",
        "Sheffield Crown Court in Sheffield", "CRT", true, "CC", null,
        listOf(
          addressFromPrisonSystem()
        )
      )
    )
    val diffs = service.updateCourtDetails(CourtUpdate("SHFCC"))
    assertThat(diffs).hasSize(1)
    val diff = diffs[0]
    assertThat(diff.areEqual()).isFalse
    assertThat(diff.entriesDiffering()?.size).isEqualTo(1)
    assertThat(diff.entriesDiffering()?.get("description")?.leftValue()).isEqualTo("Sheffield Crown Court Wibble")
    assertThat(diff.entriesDiffering()?.get("description")?.rightValue()).isEqualTo("Sheffield Crown Court")
  }

  @Test
  fun `should perform update on court when court type changed`() {
    val courtRegisterData = generateCourtRegisterEntry()

    whenever(courtRegisterService.getCourtInfoFromRegister(eq("SHFCC"))).thenReturn(courtRegisterData)

    whenever(prisonService.getCourtInformation(eq("SHFCC"))).thenReturn(
      CourtFromPrisonSystem(
        "SHFCC", "Sheffield Crown Court",
        "Sheffield Crown Court in Sheffield", "CRT", true, "MG", null,
        listOf(
          addressFromPrisonSystem()
        )
      )
    )
    val diffs = service.updateCourtDetails(CourtUpdate("SHFCC"))
    assertThat(diffs).hasSize(1)
    val diff = diffs[0]
    assertThat(diff.areEqual()).isFalse
    assertThat(diff.entriesDiffering()?.size).isEqualTo(1)
    assertThat(diff.entriesDiffering()?.get("courtType")?.leftValue()).isEqualTo("MG")
    assertThat(diff.entriesDiffering()?.get("courtType")?.rightValue()).isEqualTo("CC")
  }

  @Test
  fun `should perform update on phone only`() {
    val courtRegisterData = generateCourtRegisterEntry()

    whenever(courtRegisterService.getCourtInfoFromRegister(eq("SHFCC"))).thenReturn(courtRegisterData)

    whenever(prisonService.getCourtInformation(eq("SHFCC"))).thenReturn(
      CourtFromPrisonSystem(
        "SHFCC", "Sheffield Crown Court",
        "Sheffield Crown Court in Sheffield", "CRT", true, "CC", null,
        listOf(
          AddressFromPrisonSystem(
            56L, "Business Address", null, "Main Sheffield Court Building", "Law Street", "Kelham Island", "Sheffield",
            "S1 5TT", "South Yorkshire", "England", true, false, LocalDate.now(), null,
            listOf(PhoneFromPrisonSystem(23432L, "0114 1232311", "BUS", null), PhoneFromPrisonSystem(23437L, "0114 1232317", "FAX", null))
          )
        )
      )
    )
    val diffs = service.updateCourtDetails(CourtUpdate("SHFCC"))
    assertThat(diffs).hasSize(1)
    val diff = diffs[0]
    assertThat(diff.areEqual()).isFalse
  }

  @Test
  fun `should perform update on address only`() {
    val courtRegisterData = generateCourtRegisterEntry()

    whenever(courtRegisterService.getCourtInfoFromRegister(eq("SHFCC"))).thenReturn(courtRegisterData)

    whenever(prisonService.getCourtInformation(eq("SHFCC"))).thenReturn(
      CourtFromPrisonSystem(
        "SHFCC", "Sheffield Crown Court",
        "Sheffield Crown Court in Sheffield", "CRT", true, "CC", null,
        listOf(
          AddressFromPrisonSystem(
            56L, "Business Address", null, "Main Sheffield Court Building", "Lawson Street", "Kelham Island", "Sheffield",
            "S1 5TT", "South Yorkshire", "England", true, false, LocalDate.now(), null,
            phoneList()
          )
        )
      )
    )

    whenever(prisonService.updateAddress(eq("SHFCC"), any())).thenReturn(
      addressFromPrisonSystem()
    )
    val diffs = service.updateCourtDetails(CourtUpdate("SHFCC"))
    assertThat(diffs).hasSize(1)
    val diff = diffs[0]
    assertThat(diff.areEqual()).isFalse
  }

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

  private fun generateCourtRegisterEntryMultipleLocations() = CourtDto(
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
      ),
      BuildingDto(
        11L,
        "SHFCC",
        "SHFCC1",
        "Annex 1",
        "Law Street",
        "Kelham Island",
        "Sheffield",
        "South Yorkshire",
        "S1 5TT",
        "England",
        listOf(
          ContactDto(12L, "SHFCC", 11L, "TEL", "0114 1232812")
        )
      ),
      BuildingDto(
        31L,
        "SHFCC",
        "SHFCC2",
        "Annex 2",
        "Law Street",
        "Kelham Island",
        "Sheffield",
        "South Yorkshire",
        "S1 5TT",
        "England",
        listOf(
          ContactDto(31L, "SHFCC", 31L, "TEL", "0114 1932311"),
          ContactDto(32L, "SHFCC", 31L, "FAX", "0114 1932312")
        )
      ),
      BuildingDto(
        41L,
        "SHFCC",
        null,
        "Another Sheffield Court Building - Part of the main",
        "Law Street",
        "Kelham Island",
        "Sheffield",
        "South Yorkshire",
        "S1 5TT",
        "England",
        listOf(
          ContactDto(41L, "SHFCC", 41L, "TEL", "0114 1232318"),
        )
      )
    )
  )
}
