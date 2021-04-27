package uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.services

import com.google.common.collect.MapDifference
import com.google.common.collect.Maps
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import com.microsoft.applicationinsights.TelemetryClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.model.CourtUpdate
import java.lang.reflect.Type
import java.time.LocalDate

@Service
class CourtRegisterUpdateService(
  private val courtRegisterService: CourtRegisterService,
  private val prisonService: PrisonService,
  private val telemetryClient: TelemetryClient,
  @Value("\${registertonomis.apply-changes}") private val applyChanges: Boolean,
  private val gson: Gson
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun updateCourtDetails(court: CourtUpdate): MapDifference<String, Any>? {
    log.info("About to update court $court")
    return updateCourt(court)
  }

  private fun updateCourt(court: CourtUpdate): MapDifference<String, Any>? {
    courtRegisterService.getCourtInfoFromRegister(court.courtId)?.run {
      log.info("Found court register data {}", this)

      val updatedCourtDataFromRegister = convertToPrisonCourtData(this)
      log.debug("Transformed register data to prison data format {}", updatedCourtDataFromRegister)

      val currentCourtDataInPrisonSystem = prisonService.getCourtInformation(courtId)
      log.debug("Found prison data version of court {}", currentCourtDataInPrisonSystem)

      val currentCourtDataToCompare =
        if (currentCourtDataInPrisonSystem != null) translateToSync(currentCourtDataInPrisonSystem) else null
      val newCourtData = mergeIds(updatedCourtDataFromRegister, currentCourtDataToCompare)

      val diffs = checkForDifferences(currentCourtDataToCompare, newCourtData)
      if (!diffs.areEqual()) {
        log.info("$courtId: APPLY CHANGES=$applyChanges - Updating Prison System with court data. Changes {}", diffs)

        storeInPrisonData(currentCourtDataToCompare, newCourtData, applyChanges)
        val trackingAttributes = mapOf(
          "courtId" to courtId,
          "changes" to diffs.toString(),
          "changes-applied" to applyChanges.toString()
        )
        telemetryClient.trackEvent("HR2NU-Court-Change", trackingAttributes, null)
      } else {
        log.info("$courtId: No changes to apply")
        telemetryClient.trackEvent("HR2NU-Court-No-Change", mapOf("courtId" to courtId), null)
      }
      return diffs
    }
    return null
  }

  private fun translateToSync(courtData: CourtFromPrisonSystem) =
    CourtDataToSync(
      courtData.agencyId,
      courtData.description,
      courtData.longDescription,
      courtData.active,
      null,
      courtData.addresses.map { address ->
        AddressDataToSync(
          addressId = address.addressId,
          addressType = getRefCode("ADDR_TYPE", address.addressType),
          premise = address.premise,
          street = address.street,
          locality = address.locality,
          town = getRefCode("CITY", address.town),
          postalCode = address.postalCode,
          county = getRefCode("COUNTY", address.county),
          country = getRefCode("COUNTRY", address.country),
          primary = address.primary,
          noFixedAddress = address.noFixedAddress,
          startDate = address.startDate,
          endDate = address.endDate,
          comment = address.comment,
          phones = address.phones.map { phone ->
            PhoneFromPrisonSystem(phone.phoneId, phone.number, phone.type, phone.ext)
          }
        )
      }
    )

  private fun storeInPrisonData(
    currentCourtData: CourtDataToSync?,
    newCourtData: CourtDataToSync,
    applyChanges: Boolean = false
  ) {
    if (applyChanges) {
      val dataPayload = translateToPrisonSystemFormat(newCourtData)
      if (currentCourtData == null) {
        log.debug("Insert Court {}", dataPayload)
        prisonService.insertCourt(dataPayload)
      } else {
        if (newCourtData != currentCourtData) { // don't update if equal
          log.debug("Update Court {}", dataPayload)
          prisonService.updateCourt(dataPayload)
        }
      }
    }

    currentCourtData?.addresses?.forEach {
      if (newCourtData.addresses.find { a -> a.addressId == it.addressId } == null) {
        log.info("No match found remove address {}", it)
        if (applyChanges) prisonService.removeAddress(newCourtData.courtId, it.addressId!!)
      }
    }

    // update addresses
    newCourtData.addresses.forEach { updatedAddress ->
      val currentAddress = currentCourtData?.addresses?.find { it.addressId == updatedAddress.addressId }

      // remove phones from this address if not matched
      currentAddress?.run {
        this.phones.forEach {
          if (updatedAddress.phones.find { p -> p.phoneId == it.phoneId } == null) {
            log.info("No match found remove phone {} for address {}", it, this)
            if (applyChanges) prisonService.removePhone(newCourtData.courtId, this.addressId!!, it.phoneId!!)
          }
        }
      }

      val updatedAddressId =
        if (applyChanges) updateAddress(newCourtData.courtId, updatedAddress, currentAddress) else null

      // update phones
      updatedAddress.phones.forEach { phone ->
        val currentPhone = currentAddress?.phones?.find { it.phoneId == phone.phoneId }
        if (phone.phoneId == null) {
          if (applyChanges && updatedAddressId != null) {
            log.debug("Insert Phone {}", phone)
            prisonService.insertPhone(newCourtData.courtId, updatedAddressId, phone)
          }
        } else {
          if (phone != currentPhone) {
            if (applyChanges && updatedAddressId != null) {
              log.debug("Update Phone {}", phone)
              prisonService.updatePhone(newCourtData.courtId, updatedAddressId, phone)
            }
          }
        }
      }
    }
  }

  private fun checkForDifferences(existingRecord: CourtDataToSync?, newRecord: CourtDataToSync): MapDifference<String, Any> {
    val type: Type = object : TypeToken<Map<String, Any>>() {}.type
    val leftMap: Map<String, Any> = if (existingRecord != null) gson.fromJson(gson.toJson(existingRecord), type) else mapOf()
    val rightMap: Map<String, Any> = gson.fromJson(gson.toJson(newRecord), type)
    return Maps.difference(leftMap, rightMap)
  }

  private fun updateAddress(
    courtId: String,
    updatedAddress: AddressDataToSync,
    currentAddress: AddressDataToSync?
  ): Long? {
    val dataPayload = translateToPrisonSystemFormat(updatedAddress)
    if (dataPayload.addressId == null) {
      log.debug("Insert Address {}", dataPayload)
      return prisonService.insertAddress(courtId, dataPayload).addressId
    }

    currentAddress.run {
      if (this != updatedAddress) {
        log.debug("Update Address {}", dataPayload)
        return prisonService.updateAddress(courtId, dataPayload).addressId
      }
    }
    return dataPayload.addressId
  }

  private fun translateToPrisonSystemFormat(courtData: CourtDataToSync) =
    CourtFromPrisonSystem(
      courtData.courtId,
      courtData.description,
      courtData.longDescription,
      "CRT",
      courtData.active,
      courtData.deactivationDate,
      courtData.addresses.map {
        addressFromPrisonSystem(it)
      }
    )

  private fun addressFromPrisonSystem(it: AddressDataToSync) =
    AddressFromPrisonSystem(
      addressId = it.addressId,
      addressType = it.addressType?.code,
      premise = it.premise,
      street = it.street,
      locality = it.locality,
      town = it.town?.code,
      postalCode = it.postalCode,
      county = it.county?.code,
      country = it.country?.code,
      primary = it.primary,
      noFixedAddress = it.noFixedAddress,
      startDate = it.startDate,
      endDate = it.endDate,
      comment = it.comment,
      phones = it.phones
    )

  private fun translateToPrisonSystemFormat(addressData: AddressDataToSync) =
    addressFromPrisonSystem(addressData)

  private fun mergeIds(updatedCourtData: CourtDataToSync, legacyCourt: CourtDataToSync?): CourtDataToSync {
    if (legacyCourt == null) return updatedCourtData

    // check for equality and update Ids if perfect match
    updatedCourtData.addresses.forEach { address ->
      val matchedAddr = legacyCourt.addresses.find { a -> a == address }
      if (matchedAddr != null) {
        with(matchedAddr) {
          updateAddressAndPhone(address)
        }
      }
    }

    // search for matching addresses with just one address
    if (legacyCourt.addresses.size == 1 && updatedCourtData.addresses.size == 1 && updatedCourtData.addresses[0].addressId == null) {
      val primaryAddress = updatedCourtData.addresses[0]

      with(legacyCourt.addresses[0]) {
        log.debug("Updating primary address {}", primaryAddress)
        updateAddressAndPhone(primaryAddress)
      }
    }
    return updatedCourtData
  }

  private fun convertToPrisonCourtData(courtDto: CourtDto) =
    CourtDataToSync(
      courtDto.courtId,
      courtDto.courtName,
      courtDto.courtDescription ?: courtDto.courtName,
      courtDto.active,
      null,
      courtDto.buildings.map { building ->
        AddressDataToSync(
          addressType = getRefCode("ADDR_TYPE", "Business Address"),
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
          phones = building.contacts.map { phone ->
            PhoneFromPrisonSystem(null, phone.detail, if (phone.type == "TEL") "BUS" else phone.type, null)
          }
        )
      }
    )

  private fun getRefCode(domain: String, description: String?): ReferenceCode? {
    var ref: ReferenceCode? = null
    if (description != null) {
      ref = prisonService.lookupCodeForReferenceDescriptions(domain, description, false).firstOrNull()
      if (ref == null) {
        ref = prisonService.lookupCodeForReferenceDescriptions(domain, description, true).firstOrNull()
      }
    }
    log.debug("Searching for text '{}' in type {} - Found = {}", description, domain, ref)
    return ref
  }
}

data class CourtDataToSync(
  val courtId: String,
  val description: String,
  val longDescription: String? = null,
  val active: Boolean,
  val deactivationDate: LocalDate? = null,
  val addresses: List<AddressDataToSync> = listOf(),
) {

  override fun hashCode(): Int {
    var result = courtId.hashCode()
    result = 31 * result + description.hashCode()
    result = 31 * result + (longDescription?.hashCode() ?: 0)
    result = 31 * result + active.hashCode()
    result = 31 * result + (deactivationDate?.hashCode() ?: 0)
    return result
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as CourtDataToSync

    if (courtId != other.courtId) return false
    if (description != other.description) return false
    if (longDescription != other.longDescription) return false
    if (active != other.active) return false
    if (deactivationDate != other.deactivationDate) return false

    return true
  }
}

data class AddressDataToSync(
  var addressId: Long? = null,
  val addressType: ReferenceCode?,
  val flat: String? = null,
  val premise: String? = null,
  val street: String? = null,
  val locality: String? = null,
  val town: ReferenceCode? = null,
  val postalCode: String? = null,
  val county: ReferenceCode? = null,
  val country: ReferenceCode? = null,
  var primary: Boolean,
  val noFixedAddress: Boolean,
  var startDate: LocalDate? = null,
  var endDate: LocalDate? = null,
  val phones: List<PhoneFromPrisonSystem> = listOf(),
  var comment: String? = null
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun updateAddressAndPhone(
    address: AddressDataToSync
  ) {
    address.addressId = addressId
    address.primary = primary
    address.startDate = startDate
    address.endDate = endDate
    address.comment = comment
    log.debug("MATCH: Court Register address {} and prison court address {}", address, this)

    // update the phones
    updatePhoneIds(address, this)
  }

  private fun updatePhoneIds(
    courtRegAddress: AddressDataToSync,
    legacyAddress: AddressDataToSync
  ) {
    courtRegAddress.phones.forEach { phone ->
      val matchedPhone = legacyAddress.phones.find { p -> p == phone }
      if (matchedPhone != null) {
        phone.phoneId = matchedPhone.phoneId
        log.debug("MATCH: Court Register phones {} and prison court phones {}", phone, matchedPhone)
      }
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as AddressDataToSync

    if (premise != other.premise) return false
    if (street != other.street) return false
    if (locality != other.locality) return false
    if (town != other.town) return false
    if (county != other.county) return false
    if (country != other.country) return false
    if (postalCode != other.postalCode) return false

    return true
  }

  override fun hashCode(): Int {
    var result = premise?.hashCode() ?: 0
    result = 31 * result + (street?.hashCode() ?: 0)
    result = 31 * result + (locality?.hashCode() ?: 0)
    result = 31 * result + (town?.hashCode() ?: 0)
    result = 31 * result + (county?.hashCode() ?: 0)
    result = 31 * result + (country?.hashCode() ?: 0)
    result = 31 * result + (postalCode?.hashCode() ?: 0)
    return result
  }
}
