package uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.services

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class CourtRegisterSyncService(
  private val courtRegisterUpdateService: CourtRegisterUpdateService,
  private val prisonReferenceDataService: PrisonReferenceDataService,
  private val courtRegisterService: CourtRegisterService,
  private val prisonService: PrisonService
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sync(): SyncStatistics {

    prisonReferenceDataService.initialiseRefData(listOf("CITY", "COUNTY", "COUNTRY", "ADDR_TYPE"))

    return syncAllCourts(prisonService.getAllCourts(), courtRegisterService.getAllActiveCourts())
  }

  private fun syncAllCourts(
    prisonCourts: List<CourtFromPrisonSystem>,
    courtRegisterCourts: List<CourtDto>
  ): SyncStatistics {

    // get all the courts from the register
    val allRegisteredCourts: MutableList<CourtDataToSync> = mutableListOf()
    courtRegisterCourts.forEach {
      allRegisteredCourts.addAll(courtRegisterUpdateService.buildCourts(it, true))
    }
    val courtMap = allRegisteredCourts.associateBy { it.courtId }

    // get all active / inactive courts from NOMIS
    val allCourtsHeldInNomis =
      prisonCourts.map { courtRegisterUpdateService.translateToSync(it, true) }.associateBy { it.courtId }

    val stats = SyncStatistics()
    // matches
    courtMap.filter { c -> allCourtsHeldInNomis[c.key] != null }
      .forEach { courtRegisterUpdateService.syncCourt(allCourtsHeldInNomis[it.key], it.value, stats) }

    // new
    courtMap.filter { c -> allCourtsHeldInNomis[c.key] == null }
      .forEach { courtRegisterUpdateService.syncCourt(null, it.value, stats) }

    // not there / inactive
    allCourtsHeldInNomis.filter { c -> c.value.active && courtMap[c.key] == null }
      .forEach {
        courtRegisterUpdateService.syncCourt(
          allCourtsHeldInNomis[it.key],
          it.value.copy(active = false, deactivationDate = LocalDate.now()),
          stats
        )
      }

    return stats
  }
}
