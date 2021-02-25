package uk.gov.justice.digital.hmpps.hmppsregisterstonomisupdate

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication()
class HmppsRegistersToNomisUpdate

fun main(args: Array<String>) {
  runApplication<HmppsRegistersToNomisUpdate>(*args)
}
