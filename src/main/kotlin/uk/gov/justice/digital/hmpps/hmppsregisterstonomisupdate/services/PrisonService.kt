package uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.services

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

  private val referenceCodes = object : ParameterizedTypeReference<List<ReferenceCode>>() {
  }

  fun <T> emptyWhenConflict(exception: WebClientResponseException): Mono<T> = emptyWhen(exception, HttpStatus.CONFLICT)
  fun <T> emptyWhenNotFound(exception: WebClientResponseException): Mono<T> = emptyWhen(exception, NOT_FOUND)
  fun <T> emptyWhen(exception: WebClientResponseException, statusCode: HttpStatus): Mono<T> =
    if (exception.rawStatusCode == statusCode.value()) Mono.empty() else Mono.error(exception)

  fun getCourtInformation(courtId : String): Agency? {
    return webClient.get()
      .uri("/api/agencies/$courtId?withAddresses=true")
      .retrieve()
      .bodyToMono(Agency::class.java)
      .onErrorResume(WebClientResponseException::class.java) { emptyWhenNotFound(it) }
      .block()
  }

  fun lookupCodeForReferenceDescriptions(domain : String, description: String, wildcard : Boolean): List<ReferenceCode> {
    val result = webClient.get()
      .uri("/api/reference-domains/domains/$domain/reverse-lookup?description=$description&wildcard=$wildcard")
      .retrieve()
      .bodyToMono(referenceCodes)
      .block()!!
    return result
  }


  fun updateCourt(updatedCourt: Agency) : Agency {
    return webClient.put()
      .uri("/api/agencies/${updatedCourt.agencyId}")
      .bodyValue(updatedCourt)
      .retrieve()
      .bodyToMono(Agency::class.java)
      .onErrorResume(WebClientResponseException::class.java) { emptyWhenNotFound(it) }
      .block()!!
  }

  fun insertCourt(newCourt: Agency) : Agency {
    return webClient.post()
      .uri("/api/agencies")
      .bodyValue(newCourt)
      .retrieve()
      .bodyToMono(Agency::class.java)
      .onErrorResume(WebClientResponseException::class.java) { emptyWhenNotFound(it) }
      .onErrorResume(WebClientResponseException::class.java) { emptyWhenConflict(it) }
      .block()!!
  }

  fun updateAddress(courtId: String, addressDto: AddressDto) : AddressDto {
    return webClient.put()
      .uri("/api/agencies/${courtId}/addresses/${addressDto.addressId}")
      .bodyValue(addressDto)
      .retrieve()
      .bodyToMono(AddressDto::class.java)
      .onErrorResume(WebClientResponseException::class.java) { emptyWhenNotFound(it) }
      .block()!!
  }

  fun insertAddress(courtId: String, addressDto: AddressDto) : AddressDto {
    return webClient.post()
      .uri("/api/agencies/${courtId}/addresses")
      .bodyValue(addressDto)
      .retrieve()
      .bodyToMono(AddressDto::class.java)
      .onErrorResume(WebClientResponseException::class.java) { emptyWhenNotFound(it) }
      .block()!!
  }

  fun insertPhone(courtId: String, addressId: Long, phone: Telephone): Telephone {
    return webClient.post()
      .uri("/api/agencies/${courtId}/addresses/${addressId}/phones")
      .bodyValue(phone)
      .retrieve()
      .bodyToMono(Telephone::class.java)
      .onErrorResume(WebClientResponseException::class.java) { emptyWhenNotFound(it) }
      .block()!!
  }

  fun updatePhone(courtId: String, addressId: Long, phone: Telephone): Telephone {
    return webClient.put()
      .uri("/api/agencies/${courtId}/addresses/${addressId}/phones/${phone.phoneId}")
      .bodyValue(phone)
      .retrieve()
      .bodyToMono(Telephone::class.java)
      .onErrorResume(WebClientResponseException::class.java) { emptyWhenNotFound(it) }
      .block()!!
  }

  fun removeAddress(courtId: String, addressId: Long) {
     webClient.delete()
      .uri("/api/agencies/${courtId}/addresses/${addressId}")
      .retrieve()
      .bodyToMono(Unit::class.java)
      .onErrorResume(WebClientResponseException::class.java) { emptyWhenNotFound(it) }
      .block()
  }

  fun removePhone(courtId: String, addressId: Long, phoneId : Long) {
     webClient.delete()
      .uri("/api/agencies/${courtId}/addresses/${addressId}/phones/${phoneId}")
      .retrieve()
      .bodyToMono(Unit::class.java)
      .onErrorResume(WebClientResponseException::class.java) { emptyWhenNotFound(it) }
      .block()
  }
}


data class Agency (
  val agencyId: String,
  val description: String,
  val longDescription: String? = null,
  val agencyType: String,
  val active : Boolean,
  val deactivationDate: LocalDate? = null,
  val addresses: List<AddressDto>? = null
) {


  override fun hashCode(): Int {
    var result = agencyId.hashCode()
    result = 31 * result + description.hashCode()
    result = 31 * result + (longDescription?.hashCode() ?: 0)
    result = 31 * result + agencyType.hashCode()
    result = 31 * result + active.hashCode()
    result = 31 * result + (deactivationDate?.hashCode() ?: 0)
    return result
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Agency

    if (agencyId != other.agencyId) return false
    if (description != other.description) return false
    if (longDescription != other.longDescription) return false
    if (agencyType != other.agencyType) return false
    if (active != other.active) return false
    if (deactivationDate != other.deactivationDate) return false

    return true
  }
}


data class AddressDto (
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
  val phones: List<Telephone>? = null,
  val comment: String? = null
) {


  override fun hashCode(): Int {
    var result = addressType?.hashCode() ?: 0
    result = 31 * result + (flat?.hashCode() ?: 0)
    result = 31 * result + (premise?.hashCode() ?: 0)
    result = 31 * result + (street?.hashCode() ?: 0)
    result = 31 * result + (locality?.hashCode() ?: 0)
    result = 31 * result + (town?.hashCode() ?: 0)
    result = 31 * result + (postalCode?.hashCode() ?: 0)
    result = 31 * result + (county?.hashCode() ?: 0)
    result = 31 * result + (country?.hashCode() ?: 0)
    result = 31 * result + primary.hashCode()
    result = 31 * result + noFixedAddress.hashCode()
    result = 31 * result + (startDate?.hashCode() ?: 0)
    result = 31 * result + (endDate?.hashCode() ?: 0)
    return result
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as AddressDto

    if (addressType != other.addressType) return false
    if (flat != other.flat) return false
    if (premise != other.premise) return false
    if (street != other.street) return false
    if (locality != other.locality) return false
    if (town != other.town) return false
    if (postalCode != other.postalCode) return false
    if (county != other.county) return false
    if (country != other.country) return false
    if (primary != other.primary) return false
    if (noFixedAddress != other.noFixedAddress) return false
    if (startDate != other.startDate) return false
    if (endDate != other.endDate) return false

    return true
  }
}


data class Telephone (
  var phoneId: Long? = null,
  val number: String,
  val type: String,
  val ext: String? = null
) {


  override fun hashCode(): Int {
    var result = number.hashCode()
    result = 31 * result + type.hashCode()
    result = 31 * result + (ext?.hashCode() ?: 0)
    return result
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as Telephone

    if (number != other.number) return false
    if (type != other.type) return false
    if (ext != other.ext) return false

    return true
  }
}


data class ReferenceCode (
   val domain: String,
   val code: String,
   val description: String,
   val activeFlag: String,
   val expiredDate: LocalDate? = null
)
