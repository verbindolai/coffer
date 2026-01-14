package org.coffer.coffer2.repository

import org.coffer.coffer2.application.issue.IssueRepositoryAdapter
import org.coffer.coffer2.domain.coin.Issue
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.time.ZonedDateTime
import java.util.UUID

@Component
class IssueRepositoryAdapterImpl(
    private val issueRepository: IssueRepository,
    private val coinIssueRepository: CoinIssueRepository
) : IssueRepositoryAdapter {
    override fun save(issue: Issue): Issue {
        return issueRepository.save(IssueEntity.fromIssue(issue)).toIssue()
    }

    override fun saveAll(issues: List<Issue>): List<Issue> {
        val entities = issues.map { IssueEntity.fromIssue(it) }
        return issueRepository.saveAll(entities).map { it.toIssue() }
    }

    override fun findByCoinId(coinId: UUID): List<Issue> {
        val issueIds = coinIssueRepository.findIssueIdsByCoinId(coinId)
        if (issueIds.isEmpty()) {
            return emptyList()
        }
        return issueRepository.findAllById(issueIds).map { it.toIssue() }
    }

    override fun findByNumistaId(numistaId: String): Issue? {
        return issueRepository.findByNumistaId(numistaId)?.toIssue()
    }

    override fun existsByNumistaId(numistaId: String): Boolean {
        return issueRepository.existsByNumistaId(numistaId)
    }

    override fun linkIssuesToCoin(coinId: UUID, issueIds: List<UUID>) {
        val mappings = issueIds.map { issueId ->
            CoinIssueEntity(coinId = coinId, issueId = issueId)
        }
        coinIssueRepository.saveAll(mappings)
    }

    override fun coinHasIssues(coinId: UUID): Boolean {
        return coinIssueRepository.existsByCoinId(coinId)
    }

    override fun findIssuesByNumistaId(numistaId: String): List<Issue> {
        val issueIds = coinIssueRepository.findIssueIdsByCoinTypeNumistaId(numistaId)
        if (issueIds.isEmpty()) {
            return emptyList()
        }
        return issueRepository.findAllById(issueIds).map { it.toIssue() }
    }

    override fun updateLastPriceFetchAttempt(issueId: UUID, timestamp: ZonedDateTime) {
        val issue = issueRepository.findByIdOrNull(issueId) ?: return
        val updatedIssue = issue.copy(lastPriceFetchAttempt = timestamp)
        issueRepository.save(updatedIssue)
    }
}
