package uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.NOT_FOUND
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.time.LocalDate

@Service
class PrisonService(@Qualifier("prisonApiWebClient") private val webClient: WebClient) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val courts = object : ParameterizedTypeReference<List<CourtFromPrisonSystem>>() {
  }
  private val referenceCodes = object : ParameterizedTypeReference<List<ReferenceCode>>() {
  }

  fun <T> emptyWhenConflict(exception: WebClientResponseException): Mono<T> = emptyWhen(exception, HttpStatus.CONFLICT)
  fun <T> emptyWhenNotFound(exception: WebClientResponseException): Mono<T> = emptyWhen(exception, NOT_FOUND)
  fun <T> emptyWhen(exception: WebClientResponseException, statusCode: HttpStatus): Mono<T> =
    if (exception.rawStatusCode == statusCode.value()) Mono.empty() else Mono.error(exception)

  fun getCourtInformation(courtId: String): CourtFromPrisonSystem? {
    return webClient.get()
      .uri("/api/agencies/$courtId?withAddresses=true&activeOnly=false&skipFormatLocation=true")
      .retrieve()
      .bodyToMono(CourtFromPrisonSystem::class.java)
      .onErrorResume(WebClientResponseException::class.java) { emptyWhenNotFound(it) }
      .block()
  }

  fun getAllCourts(): List<CourtFromPrisonSystem> {
    return webClient.get()
      .uri("/api/agencies/type/CRT?withAddresses=true&activeOnly=false&skipFormatLocation=true")
      .retrieve()
      .bodyToMono(courts)
      .onErrorResume(WebClientResponseException::class.java) { emptyWhenNotFound(it) }
      .block()!!
  }

  fun lookupCodeForReferenceDescriptions(domain: String, description: String, wildcard: Boolean): List<ReferenceCode> {
    val result = webClient.get()
      .uri("/api/reference-domains/domains/$domain/reverse-lookup?description=$description&wildcard=$wildcard")
      .retrieve()
      .bodyToMono(referenceCodes)
      .block()!!
    return result
  }

  fun getReferenceData(domain: String): List<ReferenceCode> {
    val result = webClient.get()
      .uri("/api/reference-domains/domains/$domain")
      .header("Page-Limit", "10000")
      .retrieve()
      .bodyToMono(referenceCodes)
      .block()!!
    return result
  }

  fun updateCourt(updatedCourt: CourtFromPrisonSystem): CourtFromPrisonSystem {
    log.debug("Updating court information with {}", updatedCourt)
    return webClient.put()
      .uri("/api/agencies/${updatedCourt.agencyId}")
      .bodyValue(updatedCourt)
      .retrieve()
      .bodyToMono(CourtFromPrisonSystem::class.java)
      .onErrorResume(WebClientResponseException::class.java) { emptyWhenNotFound(it) }
      .block()!!
  }

  fun insertCourt(newCourt: CourtFromPrisonSystem): CourtFromPrisonSystem {
    log.debug("Inserting new court information with {}", newCourt)
    return webClient.post()
      .uri("/api/agencies")
      .bodyValue(newCourt)
      .retrieve()
      .bodyToMono(CourtFromPrisonSystem::class.java)
      .onErrorResume(WebClientResponseException::class.java) { emptyWhenNotFound(it) }
      .onErrorResume(WebClientResponseException::class.java) { emptyWhenConflict(it) }
      .block()!!
  }

  fun updateAddress(courtId: String, addressFromPrisonSystem: AddressFromPrisonSystem): AddressFromPrisonSystem {
    log.debug("Updating address information for court {} with {}", courtId, addressFromPrisonSystem)
    return webClient.put()
      .uri("/api/agencies/$courtId/addresses/${addressFromPrisonSystem.addressId}")
      .bodyValue(addressFromPrisonSystem)
      .retrieve()
      .bodyToMono(AddressFromPrisonSystem::class.java)
      .onErrorResume(WebClientResponseException::class.java) { emptyWhenNotFound(it) }
      .block()!!
  }

  fun insertAddress(courtId: String, addressFromPrisonSystem: AddressFromPrisonSystem): AddressFromPrisonSystem {
    log.debug("Inserting address information for court {} with {}", courtId, addressFromPrisonSystem)

    return webClient.post()
      .uri("/api/agencies/$courtId/addresses")
      .bodyValue(addressFromPrisonSystem)
      .retrieve()
      .bodyToMono(AddressFromPrisonSystem::class.java)
      .onErrorResume(WebClientResponseException::class.java) { emptyWhenNotFound(it) }
      .block()!!
  }

  fun insertPhone(courtId: String, addressId: Long, phone: PhoneFromPrisonSystem): PhoneFromPrisonSystem {
    log.debug("Adding new phone detail {} for address {} in court {}", phone, addressId, courtId)

    return webClient.post()
      .uri("/api/agencies/$courtId/addresses/$addressId/phones")
      .bodyValue(phone)
      .retrieve()
      .bodyToMono(PhoneFromPrisonSystem::class.java)
      .onErrorResume(WebClientResponseException::class.java) { emptyWhenNotFound(it) }
      .block()!!
  }

  fun updatePhone(courtId: String, addressId: Long, phone: PhoneFromPrisonSystem): PhoneFromPrisonSystem {
    log.debug("Updating phone detail {} for address {} in court {}", phone, addressId, courtId)

    return webClient.put()
      .uri("/api/agencies/$courtId/addresses/$addressId/phones/${phone.phoneId}")
      .bodyValue(phone)
      .retrieve()
      .bodyToMono(PhoneFromPrisonSystem::class.java)
      .onErrorResume(WebClientResponseException::class.java) { emptyWhenNotFound(it) }
      .block()!!
  }

  fun removeAddress(courtId: String, addressId: Long) {
    log.debug("Removing address from court {} with id {}", courtId, addressId)
    webClient.delete()
      .uri("/api/agencies/$courtId/addresses/$addressId")
      .retrieve()
      .bodyToMono(Unit::class.java)
      .onErrorResume(WebClientResponseException::class.java) { emptyWhenNotFound(it) }
      .block()
  }

  fun removePhone(courtId: String, addressId: Long, phoneId: Long) {
    log.debug("Removing phone from address {} in court {} with id {}", addressId, courtId, phoneId)
    webClient.delete()
      .uri("/api/agencies/$courtId/addresses/$addressId/phones/$phoneId")
      .retrieve()
      .bodyToMono(Unit::class.java)
      .onErrorResume(WebClientResponseException::class.java) { emptyWhenNotFound(it) }
      .block()
  }
}

data class CourtFromPrisonSystem(
  val agencyId: String,
  val description: String,
  val longDescription: String? = null,
  val agencyType: String,
  val active: Boolean,
  val courtType: String?,
  val deactivationDate: LocalDate? = null,
  val addresses: List<AddressFromPrisonSystem> = listOf(),
)

data class AddressFromPrisonSystem(
  var addressId: Long? = null,
  val addressType: String? = "BUS",
  val flat: String? = null,
  val premise: String? = null,
  val street: String? = null,
  val locality: String? = null,
  val town: String? = null,
  val postalCode: String? = null,
  val county: String? = null,
  val country: String? = null,
  var primary: Boolean,
  val noFixedAddress: Boolean,
  var startDate: LocalDate? = null,
  var endDate: LocalDate? = null,
  val phones: List<PhoneFromPrisonSystem> = listOf(),
  var comment: String? = null
)

data class PhoneFromPrisonSystem(
  var phoneId: Long? = null,
  val number: String,
  val type: String,
  val ext: String? = null
) : Comparable<PhoneFromPrisonSystem> {

  override fun compareTo(other: PhoneFromPrisonSystem): Int {
    return compareBy<PhoneFromPrisonSystem>({ it.number }, { it.type }, { it.ext }).compare(this, other)
  }

  override fun hashCode(): Int {
    var result = number.hashCode()
    result = 31 * result + type.hashCode()
    result = 31 * result + (ext?.hashCode() ?: 0)
    return result
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as PhoneFromPrisonSystem

    if (number != other.number) return false
    if (type != other.type) return false
    if (ext != other.ext) return false

    return true
  }
}

data class ReferenceCode(
  val domain: String,
  val code: String,
  val description: String,
  val activeFlag: String,
  val expiredDate: LocalDate? = null
) {
  override fun hashCode(): Int {
    var result = domain.hashCode()
    result = 31 * result + code.hashCode()
    result = 31 * result + description.hashCode()
    return result
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as ReferenceCode

    if (domain != other.domain) return false
    if (code != other.code) return false
    if (description != other.description) return false

    return true
  }
}
