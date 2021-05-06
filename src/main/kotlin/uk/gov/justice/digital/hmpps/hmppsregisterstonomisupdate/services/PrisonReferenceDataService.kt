package uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PrisonReferenceDataService(private val prisonService: PrisonService) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val refData : MutableMap<String, Map<String, ReferenceCode>> = mutableMapOf()

  fun initialiseRefData(domains : List<String>) {
    domains.forEach {
      refData[it] = prisonService.getReferenceData(it).map{ it.description to it }.toMap()
    }
  }

  fun getRefCode(domain: String, description: String?, useCache: Boolean = false): ReferenceCode? {
    var ref: ReferenceCode? = null
    if (description != null) {
      if (useCache) {
        ref = refData[domain]?.get(description)
      } else {
        ref = prisonService.lookupCodeForReferenceDescriptions(domain, description, false).firstOrNull()
      }
    }
    return ref
  }
}