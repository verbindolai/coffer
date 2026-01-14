package org.coffer.coffer2.repository

import org.coffer.coffer2.application.issueprice.IssuePriceRepositoryAdapter
import org.coffer.coffer2.domain.coin.CoinGrade
import org.coffer.coffer2.domain.coin.IssuePrice
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class IssuePriceRepositoryAdapterImpl(
    private val issuePriceRepository: IssuePriceRepository
) : IssuePriceRepositoryAdapter {
    override fun save(issuePrice: IssuePrice): IssuePrice {
        return issuePriceRepository.save(IssuePriceEntity.fromIssuePrice(issuePrice)).toIssuePrice()
    }

    override fun saveAll(issuePrices: List<IssuePrice>): List<IssuePrice> {
        val entities = issuePrices.map { IssuePriceEntity.fromIssuePrice(it) }
        return issuePriceRepository.saveAll(entities).map { it.toIssuePrice() }
    }

    override fun findByIssueId(issueId: UUID): List<IssuePrice> {
        return issuePriceRepository.findByIssueId(issueId).map { it.toIssuePrice() }
    }

    override fun findLatestByIssueId(issueId: UUID): List<IssuePrice> {
        return issuePriceRepository.findLatestPricesByIssueId(issueId).map { it.toIssuePrice() }
    }

    override fun findLatestByIssueIdAndGrade(issueId: UUID, grade: CoinGrade): IssuePrice? {
        return issuePriceRepository.findLatestByIssueIdAndGrade(issueId, grade)?.toIssuePrice()
    }
}
