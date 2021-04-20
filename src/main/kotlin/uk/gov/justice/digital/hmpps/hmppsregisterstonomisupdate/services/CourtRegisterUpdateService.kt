package uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.services

import com.google.gson.Gson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
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
  private val prisonService: PrisonService,
  @Value("\${registertonomis.apply-changes}") private val applyChanges: Boolean,
  private val gson: Gson
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
      log.debug("Transformed register data to prison data format {}", convertToPrisonCourtData)

      val legacyCourtInfo = prisonService.getCourtInformation(court.courtId)
      log.debug("Found prison data version of court {}", legacyCourtInfo)

      storeInPrisonData(legacyCourtInfo, mergeIds(convertToPrisonCourtData, legacyCourtInfo), applyChanges)
      log.debug("Update Completed")
    }
  }

  private fun storeInPrisonData(legacyCourtInfo: Agency?, court: Agency, applyChanges: Boolean = false) {

    if (applyChanges) {
      if (legacyCourtInfo == null) {
        prisonService.insertCourt(court)
      } else {
        if (court != legacyCourtInfo) { // don't update if equal
          prisonService.updateCourt(court)
        }
      }
    }

    legacyCourtInfo?.addresses?.forEach {
      if (court.addresses?.find { a -> a.addressId == it.addressId } == null) {
        log.info("No match found remove address {}", it)
        if (applyChanges) prisonService.removeAddress(court.agencyId, it.addressId!!)
      }
    }

    // update addresses
    court.addresses?.forEach { address ->
      log.info("Process Address {}", address)

      val updatedAddress =
        if (applyChanges) {
          if (address.addressId == null) {
            prisonService.insertAddress(
              court.agencyId,
              address
            )
          } else {
            prisonService.updateAddress(court.agencyId, address)
          }
        } else {
          address
        }

      // remove phones from this address if not matched
      legacyCourtInfo?.addresses?.find { it.addressId == address.addressId }?.run {
        this.phones?.forEach {
          if (address.phones?.find { p -> p.phoneId == it.phoneId } == null) {
            log.info("No match found remove phone {} for address {}", it, this)
            if (applyChanges) prisonService.removePhone(court.agencyId, this.addressId!!, it.phoneId!!)
          }
        }
      }

      // update phones
      address.phones?.forEach { phone ->
        log.info("Process Phone {}", phone)
        if (applyChanges) {
          if (phone.phoneId == null) prisonService.insertPhone(
            court.agencyId,
            updatedAddress.addressId!!,
            phone
          ) else prisonService.updatePhone(court.agencyId, updatedAddress.addressId!!, phone)
        }
      }
    }
  }

  private fun mergeIds(updatedCourtData: Agency, legacyCourt: Agency?): Agency {
    if (legacyCourt == null) return updatedCourtData

    // check for equality and update Ids if perfect match
    updatedCourtData.addresses?.forEach { address ->
      val matchedAddr = legacyCourt.addresses?.find { a -> a == address }
      if (matchedAddr != null) {
        with(matchedAddr) {
          log.debug("MATCH: Court Register address {} and prison court address {}", address, this)
          updateAddressAndPhone(address)
        }
      }
    }

    // search for matching addresses with just one address
    if (legacyCourt.addresses?.size == 1 && updatedCourtData.addresses?.size == 1 && updatedCourtData.addresses[0].addressId == null) {
      val primaryAddress = updatedCourtData.addresses[0]

      with(legacyCourt.addresses[0]) {
        log.debug("Updating primary address {}", primaryAddress)
        updateAddressAndPhone(primaryAddress)
      }
    }
    return updatedCourtData
  }

  private fun AddressDto.updateAddressAndPhone(
    primaryAddress: AddressDto
  ) {
    primaryAddress.addressId = addressId
    primaryAddress.primary = primary
    primaryAddress.startDate = startDate
    primaryAddress.endDate = endDate
    primaryAddress.comment = comment
    // update the phones
    updatePhoneIds(primaryAddress, this)

    log.debug("Updated to {}", primaryAddress)
  }

  private fun updatePhoneIds(
    courtRegAddress: AddressDto,
    legacyAddress: AddressDto
  ) {
    courtRegAddress.phones?.forEach { phone ->
      val matchedPhone = legacyAddress?.phones?.find { p -> p == phone }
      if (matchedPhone != null) {
        log.debug("Court Register phone {} and prison court phone match {}", phone, matchedPhone)
        phone.phoneId = matchedPhone.phoneId
      }
    }
  }

  private fun convertToPrisonCourtData(courtDto: CourtDto) =
    Agency(
      courtDto.courtId,
      courtDto.courtName,
      courtDto.courtDescription ?: courtDto.courtName,
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
          primary = courtDto.buildings.size == 1 || building.subCode != null,
          noFixedAddress = false,
          startDate = LocalDate.now(),
          endDate = null,
          comment = "Updated from Court Register",
          phones = building.contacts?.map { phone ->
            Telephone(null, phone.detail, if (phone.type == "TEL") "BUS" else phone.type, null)
          }
        )
      }
    )

  private fun getRefCode(domain: String, description: String?): String? {
    if (description != null) {
      val ref = prisonService.lookupCodeForReferenceDescriptions(domain, description, false).firstOrNull()
      log.debug("Searching for text '{}' in type {} - Found = {}", description, domain, ref)
      return ref?.code
    }
    return null
  }

  private fun getCourtInfoFromRegister(courtId: String): CourtDto? {
    log.debug("Looking up court details from register {}", courtId)
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
