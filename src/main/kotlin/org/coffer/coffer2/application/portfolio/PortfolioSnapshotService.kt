package org.coffer.coffer2.application.portfolio

import java.time.LocalDate

interface PortfolioSnapshotService {
    /**
     * Computes a snapshot of the current portfolio and stores it.
     * If a snapshot already exists for today, it will be overwritten.
     */
    fun computeAndStoreSnapshot(): PortfolioSnapshotResult

    /**
     * Gets all snapshots after a given start date.
     */
    fun getSnapshotsAfter(startDate: LocalDate): List<PortfolioSnapshotResult>

    /**
     * Gets all stored snapshots.
     */
    fun getAllSnapshots(): List<PortfolioSnapshotResult>

    /**
     * Gets the latest stored snapshot.
     */
    fun getLatestSnapshot(): PortfolioSnapshotResult?
}
