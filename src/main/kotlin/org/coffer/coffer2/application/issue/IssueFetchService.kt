package org.coffer.coffer2.application.issue

import io.github.oshai.kotlinlogging.KotlinLogging
import org.coffer.coffer2.remote.numista.NumistaClient
import org.coffer.coffer2.remote.numista.toIssue
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class IssueFetchService(
    private val issueRepositoryAdapter: IssueRepositoryAdapter,
    private val numistaClient: NumistaClient
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Fetches issues from Numista for a given coin and creates the mapping.
     * If issues already exist for this coin type (from another coin), just creates the mapping without fetching.
     *
     * @param coinId The UUID of the coin to link issues to
     * @param numistaId The Numista coin ID
     * @return true if at least one issue was successfully linked, false otherwise
     */
    @Transactional
    fun fetchIssuesFromNumista(coinId: UUID, numistaId: String): Boolean {
        logger.info { "Processing issue fetch for coin $coinId with coin type Numista ID $numistaId" }

        // Check if this coin already has linked issues
        if (issueRepositoryAdapter.coinHasIssues(coinId)) {
            logger.info { "Coin $coinId already has linked issues, skipping" }
            return false
        }

        // Check if issues already exist for this coin type (from other coins)
        val existingIssues = issueRepositoryAdapter.findIssuesByNumistaId(numistaId)

        if (existingIssues.isNotEmpty()) {
            // Reuse existing issues - just create the mappings
            logger.info { "Found ${existingIssues.size} existing issues for coin type $numistaId, reusing them" }
            val issueIds = existingIssues.mapNotNull { it.id }
            issueRepositoryAdapter.linkIssuesToCoin(coinId, issueIds)
            logger.info { "Successfully linked ${issueIds.size} existing issues to coin $coinId" }
            return true
        }

        // No issues exist yet - fetch from Numista API
        logger.info { "No existing issues found, fetching from Numista API for coin type $numistaId" }
        val numistaIssues = numistaClient.getIssues(numistaId)

        if (numistaIssues.isEmpty()) {
            logger.info { "No issues available for coin type Numista ID $numistaId" }
            return false
        }

        // Convert Numista responses to domain models and save them
        val issues = numistaIssues.map { response -> response.toIssue() }
        val savedIssues = issueRepositoryAdapter.saveAll(issues)
        logger.info { "Successfully saved ${savedIssues.size} new issues for coin type $numistaId" }

        // Link all issues to this coin
        val issueIds = savedIssues.mapNotNull { it.id }
        if (issueIds.isNotEmpty()) {
            issueRepositoryAdapter.linkIssuesToCoin(coinId, issueIds)
            logger.info { "Successfully linked ${issueIds.size} issues to coin $coinId" }
            return true
        }

        logger.warn { "No issues could be linked to coin $coinId" }
        return false
    }
}
