package uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.services

import com.google.common.collect.MapDifference
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class CourtRegisterSyncService(
  private val courtRegisterUpdateService: CourtRegisterUpdateService,
  private val courtRegisterService: CourtRegisterService,
  private val prisonService: PrisonService
) {

  companion object {
    val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun sync(): List<MapDifference<String, Any>> {
    return syncAllCourts(prisonService.getAllCourts(), courtRegisterService.getAllActiveCourts())
  }

  private fun syncAllCourts(prisonCourts: List<CourtFromPrisonSystem>, courtRegisterCourts: List<CourtDto>): List<MapDifference<String, Any>> {

    // get all the courts from the register
    val allRegisteredCourts: MutableList<CourtDataToSync> = mutableListOf()
    courtRegisterCourts.forEach {
      allRegisteredCourts.addAll(courtRegisterUpdateService.buildCourts(it))
    }
    val courtMap = allRegisteredCourts.associateBy { it.courtId }

    // get all active / inactive courts from NOMIS
    val allCourtsHeldInNomis =
      prisonCourts.map { courtRegisterUpdateService.translateToSync(it) }.associateBy { it.courtId }

    val diffs: MutableList<MapDifference<String, Any>> = mutableListOf()

    diffs.addAll(
      courtMap.filter { c -> allCourtsHeldInNomis[c.key] == null }
        .map { courtRegisterUpdateService.syncCourt(null, it.value) }
    )

    diffs.addAll(
      allCourtsHeldInNomis.filter { c -> courtMap[c.key] == null }
        .map {
          courtRegisterUpdateService.syncCourt(
            allCourtsHeldInNomis[it.key],
            it.value.copy(active = false, deactivationDate = LocalDate.now())
          )
        }
    )
    diffs.addAll(
      courtMap.filter { c -> allCourtsHeldInNomis[c.key] != null }
        .map { courtRegisterUpdateService.syncCourt(allCourtsHeldInNomis[it.key], it.value) }
    )

    return diffs
  }
}
