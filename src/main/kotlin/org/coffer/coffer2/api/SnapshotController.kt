package org.coffer.coffer2.api

import org.coffer.coffer2.application.portfolio.PortfolioSnapshotService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/snapshots")
class SnapshotController(
    private val portfolioSnapshotService: PortfolioSnapshotService
) {

    @GetMapping("/trigger")
    fun triggerSnapshot(): ResponseEntity<Void> {
        portfolioSnapshotService.computeAndStoreSnapshot()
        return ResponseEntity.ok().build()
    }
}