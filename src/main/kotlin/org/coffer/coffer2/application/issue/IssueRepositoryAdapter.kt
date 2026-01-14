package org.coffer.coffer2.application.issue

import org.coffer.coffer2.domain.coin.Issue
import java.time.ZonedDateTime
import java.util.UUID

/**
 * Repository adapter for Issue entities.
 * Provides a clean abstraction between the domain layer and persistence layer.
 */
interface IssueRepositoryAdapter {
    /**
     * Saves a single issue to the repository.
     *
     * @param issue The issue to save
     * @return The saved issue
     */
    fun save(issue: Issue): Issue

    /**
     * Saves multiple issues in a batch operation.
     *
     * @param issues The list of issues to save
     * @return The list of saved issues
     */
    fun saveAll(issues: List<Issue>): List<Issue>

    /**
     * Finds all issues for a given coin through the mapping table.
     *
     * @param coinId The UUID of the coin
     * @return List of issues associated with the coin
     */
    fun findByCoinId(coinId: UUID): List<Issue>

    /**
     * Finds issue by Numista ID.
     *
     * @param numistaId The Numista type ID
     * @return Issue if found, null otherwise
     */
    fun findByNumistaId(numistaId: String): Issue?

    /**
     * Checks if issues exist for a given Numista ID.
     *
     * @param numistaId The Numista type ID to check
     * @return true if issues exist for this Numista ID, false otherwise
     */
    fun existsByNumistaId(numistaId: String): Boolean

    /**
     * Links issues to a coin.
     *
     * @param coinId The coin UUID
     * @param issueIds The list of issue UUIDs to link
     */
    fun linkIssuesToCoin(coinId: UUID, issueIds: List<UUID>)

    /**
     * Checks if a coin has any linked issues.
     *
     * @param coinId The coin UUID
     * @return true if the coin has linked issues, false otherwise
     */
    fun coinHasIssues(coinId: UUID): Boolean

    /**
     * Finds all issues for any coin with the given Numista type ID.
     * This is used to reuse issues when multiple coins share the same type.
     *
     * @param numistaId The Numista coin type ID
     * @return List of issues, empty if no coin with this type has issues yet
     */
    fun findIssuesByNumistaId(numistaId: String): List<Issue>

    /**
     * Updates the lastPriceFetchAttempt timestamp for an issue.
     * Used to track when we last tried to fetch prices from Numista.
     *
     * @param issueId The issue UUID
     * @param timestamp The timestamp to set
     */
    fun updateLastPriceFetchAttempt(issueId: UUID, timestamp: ZonedDateTime)
}
