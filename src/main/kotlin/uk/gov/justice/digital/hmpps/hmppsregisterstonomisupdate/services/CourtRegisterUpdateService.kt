package uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.model.CourtUpdate
import java.time.LocalDate

@Service
class CourtRegisterUpdateService(
  @Qualifier("courtRegisterApiWebClient") private val webClient: WebClient,
  private val prisonService: PrisonService
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun updateCourtDetails(court: CourtUpdate) {
    log.info("About to update court $court")
    updateCourt(court)
  }

  private fun updateCourt(court: CourtUpdate) {
    getCourtInfoFromRegister(court.courtId)?.run {
      log.info("Found court register data {}", this)

      val convertToPrisonCourtData = convertToPrisonCourtData(this)
      val courtInformation = prisonService.getCourtInformation(court.courtId)

      storeInPrisonData(mergeIds(convertToPrisonCourtData, courtInformation))
    }
  }

  private fun storeInPrisonData(court: Agency) {
    val legacyCourtInfo = prisonService.getCourtInformation(courtId = court.agencyId)
    if (legacyCourtInfo == null) prisonService.insertCourt(court) else prisonService.updateCourt(court)

    legacyCourtInfo?.addresses?.forEach {
      if (court.addresses?.find { a -> a.addressId == it.addressId } == null) {
        prisonService.removeAddress(court.agencyId, it.addressId!!)
      }
    }

    // remove non mapped addresses
    legacyCourtInfo?.addresses?.forEach {
      if (court.addresses?.find { a -> a.addressId == it.addressId } == null) {
        prisonService.removeAddress(court.agencyId, it.addressId!!)
      }
    }

    // update addresses
    court.addresses?.forEach { address ->
      val updatedAddress = if (address.addressId == null) prisonService.insertAddress(court.agencyId, address) else prisonService.updateAddress(court.agencyId, address)

      // remove phones from this address if not matched
      legacyCourtInfo?.addresses?.find { it.addressId == address.addressId }?.run {
        this.phones?.forEach {
          if (address.phones?.find { p -> p.phoneId == it.phoneId } == null) {
            prisonService.removePhone(court.agencyId, this.addressId!!, it.phoneId!!)
          }
        }
      }

      //update phones
      address.phones?.forEach { phone ->
        if (phone.phoneId == null) prisonService.insertPhone(court.agencyId, updatedAddress.addressId!!, phone) else prisonService.updatePhone(court.agencyId, updatedAddress.addressId!!, phone)
      }
    }
  }

  private fun mergeIds(updatedCourtData: Agency, legacyCourt: Agency?) : Agency {
    if (legacyCourt == null) return updatedCourtData

    // check for equality and update Ids if perfect match
    updatedCourtData.addresses?.forEach { address ->
      val matchedAddr = legacyCourt.addresses?.find{ a -> a == address }
      if (matchedAddr != null) {
        address.addressId = matchedAddr.addressId
      }

      updatePhoneIds(address, matchedAddr)
    }

    // search for matching addresses with just one address
    if (legacyCourt.addresses?.size == 1 && updatedCourtData.addresses?.size == 1 && updatedCourtData.addresses[0].addressId == null) {
       updatedCourtData.addresses[0].addressId = legacyCourt.addresses[0].addressId

      // update the phones
       updatePhoneIds(updatedCourtData.addresses[0], legacyCourt.addresses[0])
    }

    return updatedCourtData;
  }

  private fun updatePhoneIds(
      courtRegAddress: AddressDto,
      legacyAddress: AddressDto?
  ) {
    courtRegAddress.phones?.forEach { phone ->
      val matchedPhone = legacyAddress?.phones?.find { p -> p == phone }
      if (matchedPhone != null) {
        phone.phoneId = matchedPhone.phoneId
      }
    }
  }

  private fun convertToPrisonCourtData(courtDto: CourtDto) =
    Agency(
      courtDto.courtId,
      courtDto.courtName,
      courtDto.courtDescription,
      "CRT",
      courtDto.active,
      null,
      courtDto.buildings?.map { building ->
        AddressDto(
          premise = building.buildingName ?: courtDto.courtName,
          street = building.street,
          locality = building.locality,
          town = getRefCode("CITY", building.town),
          postalCode = building.postcode,
          county = getRefCode("COUNTY", building.county),
          country = getRefCode("COUNTRY", building.country),
          primary = false,
          noFixedAddress = false,
          startDate = LocalDate.now(),
          endDate = null,
          comment = "Updated from Court Register",
          phones = building.contacts?.map { phone ->
            Telephone(null, phone.detail, phone.type, null)
          })
      }
    )


  private fun getRefCode(domain: String, description: String?): String? {
    if (description != null) {
      return prisonService.lookupCodeForReferenceDescriptions(domain, description, false).firstOrNull()?.code
    }
    return null
  }

  private fun getCourtInfoFromRegister(courtId: String): CourtDto? {
    return webClient.get()
      .uri("/courts/id/$courtId")
      .retrieve()
      .bodyToMono(CourtDto::class.java)
      .onErrorResume(WebClientResponseException::class.java) { emptyWhenNotFound(it) }
      .block()
  }
}

fun <T> emptyWhenNotFound(exception: WebClientResponseException): Mono<T> = emptyWhen(exception, NOT_FOUND)
fun <T> emptyWhen(exception: WebClientResponseException, statusCode: HttpStatus): Mono<T> =
  if (exception.rawStatusCode == statusCode.value()) Mono.empty() else Mono.error(exception)



data class CourtDto(
  val courtId: String,
  val courtName: String,
  val courtDescription: String?,
  val type: CourtTypeDto,
  val active: Boolean,
  val buildings: List<BuildingDto>?
)


data class CourtTypeDto(
  val courtType: String,
  val courtName: String
)


data class BuildingDto(
  val id: Long,
  val courtId: String,
  val subCode: String?,
  val buildingName: String?,
  val street: String?,
  val locality: String?,
  val town: String?,
  val county: String?,
  val postcode: String?,
  val country: String?,
  val contacts: List<ContactDto>?
)


data class ContactDto(
  val id: Long,
  val courtId: String,
  val buildingId: Long,
  val type: String,
  val detail: String
)


