package uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.Locale

@Service
class PrisonReferenceDataService(private val prisonService: PrisonService) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  private val refData: MutableMap<String, Map<String, ReferenceCode>> = mutableMapOf()

  fun initialiseRefData(domains: List<String>) {
    domains.forEach { domain ->
      refData[domain] = prisonService.getReferenceData(domain).associateBy { it.description.uppercase(Locale.getDefault()) }
    }
  }

  fun getRefCode(domain: String, description: String?, useCache: Boolean = false): ReferenceCode? {
    var ref: ReferenceCode? = null
    if (description != null) {
      ref = if (useCache) {
        refData[domain]?.get(description.uppercase(Locale.getDefault()))
      } else {
        prisonService.lookupCodeForReferenceDescriptions(domain, description, false).firstOrNull()
      }
    }
    return ref
  }
}
