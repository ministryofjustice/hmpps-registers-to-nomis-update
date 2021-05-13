package uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.services

import com.fasterxml.jackson.annotation.JsonIgnore
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
  private val prisonReferenceDataService: PrisonReferenceDataService,
  private val telemetryClient: TelemetryClient,
  @Value("\${registertonomis.apply-changes}") private val applyChanges: Boolean,
  private val gson: Gson
) {
  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val courtTypesToNOMISMap = mapOf(
    "CMT" to "GCM",
    "COA" to "CACD",
    "COU" to "CO",
    "CRN" to "CC",
    "MAG" to "MC",
    "YTH" to "YC",
    "COM" to "CB",
    "IMM" to "IMM",
    "OTH" to "OTHER"
  )

  fun updateCourtDetails(court: CourtUpdate): UpdateStatistics {
    val stats = UpdateStatistics()

    courtRegisterService.getCourtInfoFromRegister(court.courtId)?.run {

      // check if court has multiple addresses with sub codes
      buildCourts(this).forEach {
        processCourt(it, stats)
      }
    }
    return stats
  }

  fun buildCourts(courtDto: CourtDto, useCache: Boolean = false): List<CourtDataToSync> {

    if (courtDto.buildings.size < 2) {
      return listOf(convertToPrisonCourtData(courtDto, courtDto.buildings, useCache))
    }

    val subCourts = courtDto.buildings.filter { b -> b.subCode != null }
      .map {
        convertToPrisonCourtData(
          CourtDto(
            it.subCode!!,
            (courtDto.courtName + " - " + it.buildingName).truncate(40),
            (courtDto.courtName + " - " + it.buildingName).truncate(3000),
            courtDto.type,
            courtDto.active,
            listOf(it)
          ),
          listOf(it),
          useCache
        )
      }

    val mainCourt = convertToPrisonCourtData(courtDto, courtDto.buildings.filter { it.subCode == null }, useCache)
    return subCourts.plus(mainCourt)
  }

  fun String.truncate(maxChar: Int): String =
    if (this.length < maxChar) this else this.substring(0, maxChar)

  private fun processCourt(courtDto: CourtDataToSync, stats: UpdateStatistics) {

    val currentCourtDataInPrisonSystem = prisonService.getCourtInformation(courtDto.courtId)

    val currentCourtDataToCompare = if (currentCourtDataInPrisonSystem != null) translateToSync(
      currentCourtDataInPrisonSystem
    ) else null

    syncCourt(currentCourtDataToCompare, courtDto, stats)
  }

  fun syncCourt(
    currentCourtDataToCompare: CourtDataToSync?,
    newCourtData: CourtDataToSync,
    stats: UpdateStatistics
  ) {

    mergeIds(newCourtData, currentCourtDataToCompare)

    val diff = checkForDifferences(currentCourtDataToCompare, newCourtData)
    stats.diffs.add(diff)
    if (!diff.areEqual()) {
      try {
        stats.add(storeInPrisonData(currentCourtDataToCompare, newCourtData, applyChanges))
        val trackingAttributes = mapOf(
          "courtId" to newCourtData.courtId,
          "changes" to diff.toString(),
          "changes-applied" to applyChanges.toString()
        )
        telemetryClient.trackEvent("HR2NU-Court-Change", trackingAttributes, null)
      } catch (e: Exception) {
        stats.courtsFailures.add(newCourtData.courtId)
        log.error("Failed to update {} - message = {}", newCourtData.courtId, e.message)
        val trackingAttributes = mapOf(
          "courtId" to newCourtData.courtId
        )
        telemetryClient.trackEvent("HR2NU-Court-Change-Failure", trackingAttributes, null)
      }
    } else {
      telemetryClient.trackEvent("HR2NU-Court-No-Change", mapOf("courtId" to newCourtData.courtId), null)
    }
  }

  fun translateToSync(courtData: CourtFromPrisonSystem, useCache: Boolean = false) =
    CourtDataToSync(
      courtData.agencyId,
      courtData.description,
      courtData.longDescription,
      courtData.active,
      courtData.courtType ?: "OTHER",
      null,
      courtData.addresses.map { address ->
        AddressDataToSync(
          addressId = address.addressId,
          addressType = prisonReferenceDataService.getRefCode("ADDR_TYPE", address.addressType, useCache),
          premise = address.premise,
          street = address.street,
          locality = address.locality,
          town = prisonReferenceDataService.getRefCode("CITY", address.town, useCache),
          postalCode = address.postalCode,
          county = prisonReferenceDataService.getRefCode("COUNTY", address.county, useCache),
          country = prisonReferenceDataService.getRefCode("COUNTRY", address.country, useCache),
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
  ): UpdateStatistics {

    val stats = UpdateStatistics()
    if (applyChanges) {
      val dataPayload = translateToPrisonSystemFormat(newCourtData)
      if (currentCourtData == null) {
        prisonService.insertCourt(dataPayload)
        stats.courtsInserted.add(dataPayload.agencyId)
      } else {
        if (newCourtData != currentCourtData) { // don't update if equal
          prisonService.updateCourt(dataPayload)
          stats.courtsUpdated.add(dataPayload.agencyId)
        }
      }
    }

    currentCourtData?.addresses?.forEach {
      if (newCourtData.addresses.find { a -> a.addressId == it.addressId } == null) {
        if (applyChanges) {
          prisonService.removeAddress(newCourtData.courtId, it.addressId!!)
          stats.numberAddressesRemoved = stats.numberAddressesRemoved + 1
        }
      }
    }

    // update addresses
    newCourtData.addresses.forEach { updatedAddress ->
      val currentAddress = currentCourtData?.addresses?.find { it.addressId == updatedAddress.addressId }

      // remove phones from this address if not matched
      currentAddress?.run {
        this.phones.forEach {
          if (updatedAddress.phones.find { p -> p.phoneId == it.phoneId } == null) {
            if (applyChanges) {
              prisonService.removePhone(newCourtData.courtId, this.addressId!!, it.phoneId!!)
              stats.numberPhonesRemoved = stats.numberPhonesRemoved + 1
            }
          }
        }
      }

      val updatedAddressId =
        if (applyChanges) updateAddress(newCourtData.courtId, updatedAddress, currentAddress, stats) else null

      // update phones
      updatedAddress.phones.forEach { phone ->
        val currentPhone = currentAddress?.phones?.find { it.phoneId == phone.phoneId }
        if (phone.phoneId == null) {
          if (applyChanges && updatedAddressId != null) {
            prisonService.insertPhone(newCourtData.courtId, updatedAddressId, phone)
            stats.numberPhonesInserted = stats.numberPhonesInserted + 1
          }
        } else {
          if (phone != currentPhone) {
            if (applyChanges && updatedAddressId != null) {
              prisonService.updatePhone(newCourtData.courtId, updatedAddressId, phone)
              stats.numberPhonesUpdated = stats.numberPhonesUpdated + 1
            }
          }
        }
      }
    }
    return stats
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
    currentAddress: AddressDataToSync?,
    stats: UpdateStatistics
  ): Long? {
    val dataPayload = translateToPrisonSystemFormat(updatedAddress)
    if (dataPayload.addressId == null) {
      val addressId = prisonService.insertAddress(courtId, dataPayload).addressId
      stats.numberAddressesInserted = stats.numberAddressesInserted + 1
      return addressId
    }

    currentAddress.run {
      if (this != updatedAddress) {
        val addressId = prisonService.updateAddress(courtId, dataPayload).addressId
        stats.numberAddressesUpdated = stats.numberAddressesUpdated + 1
        return addressId
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
      courtData.courtType,
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

  private fun mergeIds(updatedCourtData: CourtDataToSync, legacyCourt: CourtDataToSync?) {
    if (legacyCourt == null) return

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
        updateAddressAndPhone(primaryAddress)
      }
    }
  }

  private fun convertToPrisonCourtData(courtDto: CourtDto, buildings: List<BuildingDto>, useCache: Boolean = false) =
    CourtDataToSync(
      courtDto.courtId,
      courtDto.courtName,
      courtDto.courtDescription ?: courtDto.courtName,
      courtDto.active,
      courtTypesToNOMISMap.getOrDefault(courtDto.type.courtType, "OTHER"),
      null,
      buildings.map { building ->
        AddressDataToSync(
          addressType = prisonReferenceDataService.getRefCode("ADDR_TYPE", "Business Address", useCache),
          premise = building.buildingName ?: courtDto.courtName,
          street = building.street,
          locality = building.locality,
          town = prisonReferenceDataService.getRefCode("CITY", building.town, useCache),
          postalCode = building.postcode,
          county = prisonReferenceDataService.getRefCode("COUNTY", building.county, useCache),
          country = prisonReferenceDataService.getRefCode("COUNTRY", building.country, useCache),
          primary = building == buildings[0], // first one in the list?
          noFixedAddress = false,
          startDate = LocalDate.now(),
          endDate = null,
          comment = null,
          phones = building.contacts.map { phone ->
            PhoneFromPrisonSystem(null, phone.detail, if (phone.type == "TEL") "BUS" else phone.type, null)
          }
        )
      }
    )
}

data class UpdateStatistics(
  var courtsInserted: MutableList<String> = mutableListOf(),
  var courtsUpdated: MutableList<String> = mutableListOf(),
  var courtsFailures: MutableList<String> = mutableListOf(),
  var numberAddressesInserted: Int = 0,
  var numberAddressesUpdated: Int = 0,
  var numberAddressesRemoved: Int = 0,
  var numberPhonesInserted: Int = 0,
  var numberPhonesUpdated: Int = 0,
  var numberPhonesRemoved: Int = 0,
  @JsonIgnore
  var diffs: MutableList<MapDifference<String, Any>> = mutableListOf()
) {

  fun add(toAdd: UpdateStatistics) {
    courtsInserted.addAll(toAdd.courtsInserted)
    courtsUpdated.addAll(toAdd.courtsUpdated)
    courtsFailures.addAll(toAdd.courtsFailures)
    numberAddressesInserted += toAdd.numberAddressesInserted
    numberAddressesUpdated += toAdd.numberAddressesUpdated
    numberAddressesRemoved += toAdd.numberAddressesRemoved
    numberPhonesInserted += toAdd.numberPhonesInserted
    numberPhonesUpdated += toAdd.numberPhonesUpdated
    numberPhonesRemoved += toAdd.numberPhonesRemoved
    diffs.addAll(toAdd.diffs)
  }
}

data class CourtDataToSync(
  val courtId: String,
  val description: String,
  val longDescription: String? = null,
  val active: Boolean,
  val courtType: String,
  val deactivationDate: LocalDate? = null,
  val addresses: List<AddressDataToSync> = listOf(),
) {

  override fun hashCode(): Int {
    var result = courtId.hashCode()
    result = 31 * result + description.hashCode()
    result = 31 * result + (longDescription?.hashCode() ?: 0)
    result = 31 * result + active.hashCode()
    result = 31 * result + courtType.hashCode()
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
    if (courtType != other.courtType) return false
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

  fun updateAddressAndPhone(
    address: AddressDataToSync
  ) {
    address.addressId = addressId
    address.primary = primary
    address.startDate = startDate
    address.endDate = endDate
    address.comment = comment

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
    if (primary != other.primary) return false

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
    result = 31 * result + primary.hashCode()
    return result
  }
}
