[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-registers-to-nomis-update/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-registers-to-nomis-update)
[![Docker Repository on Quay](https://quay.io/repository/hmpps/hmpps-registers-to-nomis-update/status "Docker Repository on Quay")](https://quay.io/repository/hmpps/hmpps-registers-to-nomis-update)
[![Runbook](https://img.shields.io/badge/runbook-view-172B4D.svg?logo=confluence)](https://dsdmoj.atlassian.net/wiki/spaces/NOM/pages/1739325587/DPS+Runbook)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://registers-to-nomis-update-dev.hmpps.service.justice.gov.uk/swagger-ui.html)

# hmpps-registers-to-nomis-update

**Handles HMPPS Domain events for registers NOMIS updates.**

The purpose of this service is to handle HMPPS Domain events for registers NOMIS updates. This listens for domain events related to changes that affect register data in NOMIS.
It will interrogate the event and will possibly request further information from the publishing service. Using that information with will call Prison API to apply any updates to NOMIS. 
