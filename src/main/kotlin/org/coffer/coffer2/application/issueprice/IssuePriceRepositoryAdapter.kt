package org.coffer.coffer2.application.issueprice

import org.coffer.coffer2.domain.coin.CoinGrade
import org.coffer.coffer2.domain.coin.IssuePrice
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Repository adapter for IssuePrice entities.
 * Provides a clean abstraction between the domain layer and persistence layer.
 */
interface IssuePriceRepositoryAdapter {
    /**
     * Saves a single issue price to the repository.
     *
     * @param issuePrice The issue price to save
     * @return The saved issue price
     */
    fun save(issuePrice: IssuePrice): IssuePrice

    /**
     * Saves multiple issue prices in a batch operation.
     *
     * @param issuePrices The list of issue prices to save
     * @return The list of saved issue prices
     */
    fun saveAll(issuePrices: List<IssuePrice>): List<IssuePrice>

    /**
     * Finds all prices for a given issue.
     *
     * @param issueId The UUID of the issue
     * @return List of prices for the issue
     */
    fun findByIssueId(issueId: UUID): List<IssuePrice>

    /**
     * Finds the latest prices for each grade for a given issue.
     *
     * @param issueId The UUID of the issue
     * @return List of latest prices per grade
     */
    fun findLatestByIssueId(issueId: UUID): List<IssuePrice>

    /**
     * Finds the latest price for a specific issue and grade combination.
     *
     * @param issueId The UUID of the issue
     * @param grade The coin grade
     * @return The latest issue price, or null if not found
     */
    fun findLatestByIssueIdAndGrade(issueId: UUID, grade: CoinGrade): IssuePrice?

    /**
     * Checks if prices have been fetched for an issue since the specified time.
     * Used to implement the "once-per-day" update limit.
     *
     * @param issueId The UUID of the issue
     * @param since The timestamp to check from
     * @return true if prices exist since the specified time, false otherwise
     */
    fun hasRecentPrices(issueId: UUID, since: ZonedDateTime): Boolean
}
