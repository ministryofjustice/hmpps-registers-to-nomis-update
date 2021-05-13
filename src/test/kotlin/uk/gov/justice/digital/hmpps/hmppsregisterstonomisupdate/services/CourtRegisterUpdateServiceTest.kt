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
  private val prisonReferenceDataService: PrisonReferenceDataService = mock()
  private val prisonService: PrisonService = mock()
  private val telemetryClient: TelemetryClient = mock()

  private lateinit var service: CourtRegisterUpdateService

  @BeforeEach
  fun before() {
    service = CourtRegisterUpdateService(courtRegisterService, prisonService, prisonReferenceDataService, telemetryClient, true, GsonConfig().gson())

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
            listOf(PhoneFromPrisonSystem(23432L, "0114 1232311", "BUS", null), PhoneFromPrisonSystem(23437L, "0114 1232317", "FAX", null))
          )
        )
      )
    )
    val stats = service.updateCourtDetails(CourtUpdate("SHFCC"))
    assertThat(stats.courts).hasSize(1)
    val courtStats = stats.courts["SHFCC"]!!
    assertThat(courtStats.differences).isEqualTo("not equal: value differences={addresses=([{addressId=56.0, addressType={domain=ADDR_TYPE, code=BUS, description=Business Address, activeFlag=Y}, premise=Main Sheffield Court Building, street=Law Street, locality=Kelham Island, town={domain=CITY, code=892734, description=Sheffield, activeFlag=Y}, postalCode=S1 5TT, county={domain=COUNTY, code=S.YORKSHIRE, description=South Yorkshire, activeFlag=Y}, country={domain=COUNTRY, code=ENG, description=England, activeFlag=Y}, primary=true, noFixedAddress=false, startDate=2021-05-13, phones=[{phoneId=23432.0, number=0114 1232311, type=BUS}, {phoneId=23437.0, number=0114 1232317, type=FAX}]}], [{addressId=56.0, addressType={domain=ADDR_TYPE, code=BUS, description=Business Address, activeFlag=Y}, premise=Main Sheffield Court Building, street=Law Street, locality=Kelham Island, town={domain=CITY, code=892734, description=Sheffield, activeFlag=Y}, postalCode=S1 5TT, county={domain=COUNTY, code=S.YORKSHIRE, description=South Yorkshire, activeFlag=Y}, country={domain=COUNTRY, code=ENG, description=England, activeFlag=Y}, primary=true, noFixedAddress=false, startDate=2021-05-13, phones=[{phoneId=23432.0, number=0114 1232311, type=BUS}, {number=0114 1232312, type=FAX}]}])}")
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
    val stats = service.updateCourtDetails(CourtUpdate("SHFCC"))
    assertThat(stats.courts).hasSize(1)
    val courtStats = stats.courts["SHFCC"]!!
    assertThat(courtStats.numberPhonesInserted).isEqualTo(0)
    assertThat(courtStats.numberPhonesRemoved).isEqualTo(0)
    assertThat(courtStats.numberAddressesInserted).isEqualTo(0)
    assertThat(courtStats.numberAddressesUpdated).isEqualTo(1)
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
