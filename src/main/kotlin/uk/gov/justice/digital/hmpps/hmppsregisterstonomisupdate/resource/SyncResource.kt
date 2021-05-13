package uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.config.ErrorResponse
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.services.CourtRegisterSyncService
import uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate.services.SyncStatistics

@RestController
@Validated
@RequestMapping(name = "Sync To NOMIS", path = ["/sync"], produces = [MediaType.APPLICATION_JSON_VALUE])
class SyncResource(
  private val courtRegisterSyncService: CourtRegisterSyncService
) {
  @PreAuthorize("hasRole('ROLE_MAINTAIN_REF_DATA') and hasAuthority('SCOPE_write')")
  @Operation(
    summary = "Update all court details",
    description = "Updates court information, role required is MAINTAIN_REF_DATA",
    security = [SecurityRequirement(name = "MAINTAIN_REF_DATA", scopes = ["write"])],
    responses = [
      ApiResponse(
        responseCode = "200",
        description = "Court Information Updated",
        content = [Content(mediaType = "application/json")]
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorized to access this endpoint",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
      ApiResponse(
        responseCode = "403",
        description = "Incorrect permissions to make court update",
        content = [Content(mediaType = "application/json", schema = Schema(implementation = ErrorResponse::class))]
      ),
    ]
  )
  @PutMapping("")
  fun syncCourts(): SyncStatistics {
    return courtRegisterSyncService.sync()
  }
}
