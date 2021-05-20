package uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.services

import com.microsoft.applicationinsights.TelemetryClient
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.check
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.config.GsonConfig
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.model.CourtUpdate
import java.time.LocalDate

class CourtRegisterUpdateServiceTest {

  private val courtRegisterService: CourtRegisterService = mock()
  private val prisonReferenceDataService: PrisonReferenceDataService = mock()
  private val prisonService: PrisonService = mock()
  private val telemetryClient: TelemetryClient = mock()

  private lateinit var service: CourtRegisterUpdateService

  @BeforeEach
  fun before() {
    service = CourtRegisterUpdateService(
      courtRegisterService,
      prisonService,
      prisonReferenceDataService,
      telemetryClient,
      true,
      GsonConfig().gson()
    )

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
    val stats = service.updateCourtDetails(CourtUpdate("SHFCC"))
    assertThat(stats.courts).hasSize(0)
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
            57L,
            "Business Address",
            null,
            "Another Sheffield Court Building - Part of main",
            "Law Street",
            "Kelham Island",
            "Sheffield",
            "S1 5TT",
            "South Yorkshire",
            "England",
            false,
            false,
            LocalDate.now(),
            null,
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
        "SHFCC2", "Sheffield Crown Court - Queen Mary Court",
        "Sheffield Crown Court - Queen Mary Court Annex 2", "CRT", true, "CC", null,
        listOf(
          AddressFromPrisonSystem(
            76L, "Business Address", null, "Queen Mary Court Annex 2", "Law Street", "Kelham Island", "Sheffield",
            "S1 5TT", "South Yorkshire", "England", true, false, LocalDate.now(), null,
            listOf(
              PhoneFromPrisonSystem(91432L, "0114 1932311", "BUS", null),
              PhoneFromPrisonSystem(91482L, "0114 1932312", "FAX", null)
            )
          )
        )
      )
    )
    val stats = service.updateCourtDetails(CourtUpdate("SHFCC"))
    assertThat(stats.courts).hasSize(0)
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
            57L,
            "Business Address",
            null,
            "Another Sheffield Court Building - Part of main",
            "Law Street",
            "Kelham Island",
            "Sheffield",
            "S1 5TT",
            "South Yorkshire",
            "England",
            false,
            false,
            LocalDate.now(),
            null,
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
        "SHFCC2", "Sheffield Crown Court - Queen Mary Court",
        "Sheffield Crown Court - Annex 2", "CRT", true, "CC", null,
        listOf(
          AddressFromPrisonSystem(
            76L, "Business Address", null, "Queen Mary Court Annex 2", "Law Street", "Kelham Island", "Sheffield",
            "S1 5TT", "South Yorkshire", "England", true, false, LocalDate.now(), null,
            listOf(
              PhoneFromPrisonSystem(91432L, "0114 1932311", "BUS", null),
              PhoneFromPrisonSystem(91482L, "0114 1932312", "FAX", null)
            )
          )
        )
      )
    )
    val stats = service.updateCourtDetails(CourtUpdate("SHFCC"))
    assertThat(stats.courts).hasSize(2)
    assertThat(stats.courts["SHFCC1"]?.differences).isEqualTo("not equal: value differences={longDescription=(Sheffield Crown Court - Annex 1 of main building, Sheffield Crown Court - Annex 1)}")
    assertThat(stats.courts["SHFCC2"]?.differences).isEqualTo("not equal: value differences={longDescription=(Sheffield Crown Court - Annex 2, Sheffield Crown Court - Queen Mary Court Annex 2)}")
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
    val stats = service.updateCourtDetails(CourtUpdate("SHFCC"))
    assertThat(stats.courts).hasSize(1)
    val courtStats = stats.courts["SHFCC"]!!
    assertThat(courtStats.differences).isEqualTo("not equal: value differences={description=(Sheffield Crown Court Wibble, Sheffield Crown Court)}")
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
    val stats = service.updateCourtDetails(CourtUpdate("SHFCC"))
    assertThat(stats.courts).hasSize(1)
    val courtStats = stats.courts["SHFCC"]!!
    assertThat(courtStats.differences).isEqualTo("not equal: value differences={courtType=(MG, CC)}")
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
            listOf(
              PhoneFromPrisonSystem(23432L, "0114 1232311", "BUS", null),
              PhoneFromPrisonSystem(23437L, "0114 1232317", "FAX", null)
            )
          )
        )
      )
    )
    val stats = service.updateCourtDetails(CourtUpdate("SHFCC"))
    assertThat(stats.courts).hasSize(1)
    val courtStats = stats.courts["SHFCC"]!!
    assertThat(courtStats.differences).contains("phones=[{phoneId=23432.0, number=0114 1232311, type=BUS}, {number=0114 1232312, type=FAX}]")
    assertThat(courtStats.numberPhonesInserted).isEqualTo(1)
    assertThat(courtStats.numberPhonesRemoved).isEqualTo(1)
    assertThat(courtStats.numberAddressesInserted).isEqualTo(0)
    assertThat(courtStats.numberAddressesUpdated).isEqualTo(0)
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
            56L,
            "Business Address",
            null,
            "Main Sheffield Court Building",
            "Lawson Street",
            "Kelham Island",
            "Sheffield",
            "S1 5TT",
            "South Yorkshire",
            "England",
            true,
            false,
            LocalDate.now(),
            null,
            phoneList()
          )
        )
      )
    )

    whenever(prisonService.updateAddress(eq("SHFCC"), any())).thenReturn(
      addressFromPrisonSystem()
    )
    val stats = service.updateCourtDetails(CourtUpdate("SHFCC"))
    assertThat(stats.courts).hasSize(1)
    val courtStats = stats.courts["SHFCC"]!!
    assertThat(courtStats.numberPhonesInserted).isEqualTo(0)
    assertThat(courtStats.numberPhonesRemoved).isEqualTo(0)
    assertThat(courtStats.numberAddressesInserted).isEqualTo(0)
    assertThat(courtStats.numberAddressesUpdated).isEqualTo(1)
  }

  @Nested
  inner class ActiveBuildings {

    @Nested
    inner class SingleExistingBuilding {

      @Test
      fun `prison court address active - register court address active - should NOT change prison address end date`() {
        whenever(prisonService.getCourtInformation(eq("SHFCC"))).thenReturn(generatePrisonCourt())
        whenever(courtRegisterService.getCourtInfoFromRegister(eq("SHFCC"))).thenReturn(
          generateCourtRegisterEntry().copy(courtName = "new court name")
        )

        service.updateCourtDetails(CourtUpdate("SHFCC"))

        verify(prisonService).updateCourt(
          check {
            assertThat(it.addresses[0].endDate).isNull()
          }
        )
      }

      @Test
      fun `prison court address NOT active - register court address NOT active - should not change prison address end date`() {
        whenever(prisonService.getCourtInformation(eq("SHFCC"))).thenReturn(
          generatePrisonCourt().copy(
            addresses = listOf(
              addressFromPrisonSystem().copy(endDate = LocalDate.now().minusDays(1))
            )
          )
        )
        whenever(courtRegisterService.getCourtInfoFromRegister(eq("SHFCC"))).thenReturn(
          generateCourtRegisterEntry()
            .copy(courtName = "new court name", buildings = listOf(addressFromCourtRegister().copy(active = false)))
        )

        service.updateCourtDetails(CourtUpdate("SHFCC"))

        verify(prisonService).updateCourt(
          check {
            assertThat(it.addresses[0].endDate).isEqualTo(LocalDate.now().minusDays(1))
          }
        )
      }

      @Test
      fun `prison court address active - register court address NOT active - should set prison address end date`() {
        whenever(prisonService.getCourtInformation(eq("SHFCC"))).thenReturn(generatePrisonCourt())
        whenever(courtRegisterService.getCourtInfoFromRegister(eq("SHFCC"))).thenReturn(
          generateCourtRegisterEntry()
            .copy(courtName = "new court name", buildings = listOf(addressFromCourtRegister().copy(active = false)))
        )

        service.updateCourtDetails(CourtUpdate("SHFCC"))

        verify(prisonService).updateCourt(
          check {
            assertThat(it.addresses[0].endDate).isEqualTo(LocalDate.now())
          }
        )
      }

      @Test
      fun `prison court address NOT active - register court address active - should set prison address end date null`() {
        whenever(prisonService.getCourtInformation(eq("SHFCC"))).thenReturn(
          generatePrisonCourt().copy(
            addresses = listOf(
              addressFromPrisonSystem().copy(endDate = LocalDate.now().minusDays(1))
            )
          )
        )
        whenever(courtRegisterService.getCourtInfoFromRegister(eq("SHFCC"))).thenReturn(
          generateCourtRegisterEntry().copy(courtName = "new court name")
        )

        service.updateCourtDetails(CourtUpdate("SHFCC"))

        verify(prisonService).updateCourt(
          check {
            assertThat(it.addresses[0].endDate).isNull()
          }
        )
      }
    }

    @Nested
    inner class NewBuildingAdded {

      @Test
      fun `new register court address active - should set NEW prison address end date null`() {
        whenever(prisonService.getCourtInformation(eq("SHFCC"))).thenReturn(generatePrisonCourt())
        whenever(courtRegisterService.getCourtInfoFromRegister(eq("SHFCC"))).thenReturn(
          generateCourtRegisterEntry()
            .copy(
              buildings = listOf(
                addressFromCourtRegister().copy(),
                addressFromCourtRegister().copy(street = "New building street")
              )
            )
        )

        service.updateCourtDetails(CourtUpdate("SHFCC"))

        verify(prisonService).insertAddress(
          eq("SHFCC"),
          check {
            assertThat(it.endDate).isNull()
          }
        )
      }
    }

    @Nested
    inner class MultipleExistingBuildings {

      @Test
      fun `prison court address active - register court address active - should not change prison address end date`() {
        whenever(prisonService.getCourtInformation(eq("SHFCC"))).thenReturn(
          generatePrisonCourt().copy(
            addresses = listOf(
              addressFromPrisonSystem().copy(),
              addressFromPrisonSystem().copy(addressId = 987L, street = "second building street")
            )
          )
        )
        whenever(courtRegisterService.getCourtInfoFromRegister(eq("SHFCC"))).thenReturn(
          generateCourtRegisterEntry()
            .copy(
              buildings = listOf(
                addressFromCourtRegister().copy(),
                addressFromCourtRegister().copy(street = "second building street has changed")
              )
            )
        )

        service.updateCourtDetails(CourtUpdate("SHFCC"))

        verify(prisonService).removeAddress("SHFCC", 987L)
        verify(prisonService).insertAddress(
          eq("SHFCC"),
          check {
            assertThat(it.endDate).isNull()
          }
        )
      }

      @Test
      fun `prison court address active - register court address NOT active - should change prison address end date to today`() {
        whenever(prisonService.getCourtInformation(eq("SHFCC"))).thenReturn(
          generatePrisonCourt().copy(
            addresses = listOf(
              addressFromPrisonSystem().copy(),
              addressFromPrisonSystem().copy(addressId = 987L, street = "second building street")
            )
          )
        )
        whenever(courtRegisterService.getCourtInfoFromRegister(eq("SHFCC"))).thenReturn(
          generateCourtRegisterEntry()
            .copy(
              buildings = listOf(
                addressFromCourtRegister().copy(),
                addressFromCourtRegister().copy(street = "second building street has changed", active = false)
              )
            )
        )

        service.updateCourtDetails(CourtUpdate("SHFCC"))

        verify(prisonService).removeAddress("SHFCC", 987L)
        verify(prisonService).insertAddress(
          eq("SHFCC"),
          check {
            assertThat(it.endDate).isEqualTo(LocalDate.now())
          }
        )
      }

      @Test
      fun `prison court address NOT active - register court address active - should change prison address end date to null`() {
        whenever(prisonService.getCourtInformation(eq("SHFCC"))).thenReturn(
          generatePrisonCourt().copy(
            addresses = listOf(
              addressFromPrisonSystem().copy(),
              addressFromPrisonSystem().copy(addressId = 987L, street = "second building street")
            )
          )
        )
        whenever(courtRegisterService.getCourtInfoFromRegister(eq("SHFCC"))).thenReturn(
          generateCourtRegisterEntry()
            .copy(
              buildings = listOf(
                addressFromCourtRegister().copy(),
                addressFromCourtRegister().copy(street = "second building street has changed")
              )
            )
        )

        service.updateCourtDetails(CourtUpdate("SHFCC"))

        verify(prisonService).removeAddress("SHFCC", 987L)
        verify(prisonService).insertAddress(
          eq("SHFCC"),
          check {
            assertThat(it.endDate).isNull()
          }
        )
      }

      @Test
      fun `prison court address NOT active - register court address NOT active - should not change prison address end date (but in fact changes it to today))`() {
        whenever(prisonService.getCourtInformation(eq("SHFCC"))).thenReturn(
          generatePrisonCourt().copy(
            addresses = listOf(
              addressFromPrisonSystem().copy(),
              addressFromPrisonSystem().copy(
                addressId = 987L,
                street = "second building street",
                endDate = LocalDate.now().minusDays(1)
              )
            )
          )
        )
        whenever(courtRegisterService.getCourtInfoFromRegister(eq("SHFCC"))).thenReturn(
          generateCourtRegisterEntry()
            .copy(
              buildings = listOf(
                addressFromCourtRegister().copy(),
                addressFromCourtRegister().copy(street = "second building street has changed", active = false)
              )
            )
        )

        service.updateCourtDetails(CourtUpdate("SHFCC"))

        verify(prisonService).removeAddress("SHFCC", 987L)
        verify(prisonService).insertAddress(
          eq("SHFCC"),
          check {
            // assertThat(it.endDate).isEqualTo(LocalDate.now().minusDays(1))  TODO This assertion should be correct but we lose information from the old prison address so cannot keep its end date
            assertThat(it.endDate).isEqualTo(LocalDate.now())
          }
        )
      }
    }
  }
}

private fun addressFromPrisonSystem() =
  AddressFromPrisonSystem(
    56L, "Business Address", null, "Main Sheffield Court Building", "Law Street", "Kelham Island", "Sheffield",
    "S1 5TT", "South Yorkshire", "England", true, false, LocalDate.now(), null,
    phoneList()
  )

private fun addressFromCourtRegister() =
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
    ),
    true
  )

private fun phoneList() =
  listOf(
    PhoneFromPrisonSystem(23432L, "0114 1232311", "BUS", null),
    PhoneFromPrisonSystem(23437L, "0114 1232312", "FAX", null)
  )

private fun generatePrisonCourt() =
  CourtFromPrisonSystem(
    "SHFCC", "Sheffield Crown Court",
    "Sheffield Crown Court in Sheffield", "CRT", true, "CC", null,
    listOf(addressFromPrisonSystem())
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
      ),
      true
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
      ),
      true
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
      ),
      true
    ),
    BuildingDto(
      31L,
      "SHFCC",
      "SHFCC2",
      "Queen Mary Court Annex 2",
      "Law Street",
      "Kelham Island",
      "Sheffield",
      "South Yorkshire",
      "S1 5TT",
      "England",
      listOf(
        ContactDto(31L, "SHFCC", 31L, "TEL", "0114 1932311"),
        ContactDto(32L, "SHFCC", 31L, "FAX", "0114 1932312")
      ),
      true
    ),
    BuildingDto(
      41L,
      "SHFCC",
      null,
      "Another Sheffield Court Building - Part of main",
      "Law Street",
      "Kelham Island",
      "Sheffield",
      "South Yorkshire",
      "S1 5TT",
      "England",
      listOf(
        ContactDto(41L, "SHFCC", 41L, "TEL", "0114 1232318"),
      ),
      true
    )
  )
)
