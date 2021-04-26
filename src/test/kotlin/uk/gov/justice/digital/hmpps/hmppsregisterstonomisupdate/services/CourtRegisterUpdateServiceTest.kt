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
        "Sheffield Crown Court in Sheffield", "CRT", true, null,
        listOf(
          addressFromPrisonSystem()
        )
      )
    )
    val diffs = service.updateCourtDetails(CourtUpdate("SHFCC"))
    assertThat(diffs?.areEqual()).isTrue
  }

  @Test
  fun `should perform update on court only`() {
    val courtRegisterData = generateCourtRegisterEntry()

    whenever(courtRegisterService.getCourtInfoFromRegister(eq("SHFCC"))).thenReturn(courtRegisterData)

    whenever(prisonService.getCourtInformation(eq("SHFCC"))).thenReturn(
      CourtFromPrisonSystem(
        "SHFCC", "Sheffield Crown Court Wibble",
        "Sheffield Crown Court in Sheffield", "CRT", true, null,
        listOf(
          addressFromPrisonSystem()
        )
      )
    )
    val diffs = service.updateCourtDetails(CourtUpdate("SHFCC"))
    assertThat(diffs?.areEqual()).isFalse
    assertThat(diffs?.entriesDiffering()?.size).isEqualTo(1)
  }

  @Test
  fun `should perform update on phone only`() {
    val courtRegisterData = generateCourtRegisterEntry()

    whenever(courtRegisterService.getCourtInfoFromRegister(eq("SHFCC"))).thenReturn(courtRegisterData)

    whenever(prisonService.getCourtInformation(eq("SHFCC"))).thenReturn(
      CourtFromPrisonSystem(
        "SHFCC", "Sheffield Crown Court",
        "Sheffield Crown Court in Sheffield", "CRT", true, null,
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
    assertThat(diffs?.areEqual()).isFalse
  }

  @Test
  fun `should perform update on address only`() {
    val courtRegisterData = generateCourtRegisterEntry()

    whenever(courtRegisterService.getCourtInfoFromRegister(eq("SHFCC"))).thenReturn(courtRegisterData)

    whenever(prisonService.getCourtInformation(eq("SHFCC"))).thenReturn(
      CourtFromPrisonSystem(
        "SHFCC", "Sheffield Crown Court",
        "Sheffield Crown Court in Sheffield", "CRT", true, null,
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
    assertThat(diffs?.areEqual()).isFalse
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
}
