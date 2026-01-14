package org.coffer.coffer2.application.issueprice

import feign.FeignException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.coffer.coffer2.application.CoinRepositoryAdapter
import org.coffer.coffer2.application.issue.IssueRepositoryAdapter
import org.coffer.coffer2.config.IssuePriceProperties
import org.coffer.coffer2.domain.coin.Coin
import org.coffer.coffer2.domain.coin.CoinGrade
import org.coffer.coffer2.domain.coin.Issue
import org.coffer.coffer2.domain.coin.IssuePrice
import org.coffer.coffer2.remote.numista.NumistaClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Service for fetching issue prices from the Numista API.
 *
 * Issue Selection Logic:
 * - Filters issues by coin's year (mandatory)
 * - If coin has mintMark → fetches prices only for matching mintLetter issues
 * - If coin has NO mintMark → fetches prices for ALL mint variations for that year
 *
 * Price Storage Logic:
 * - If coin has grade → stores only prices for that specific grade
 * - If coin has NO grade → stores prices for ALL grades
 *
 * This ensures optimal price coverage when coin data is incomplete.
 */
@Service
class IssuePriceFetchService(
    private val coinRepositoryAdapter: CoinRepositoryAdapter,
    private val issueRepositoryAdapter: IssueRepositoryAdapter,
    private val issuePriceRepositoryAdapter: IssuePriceRepositoryAdapter,
    private val numistaClient: NumistaClient,
    private val issuePriceProperties: IssuePriceProperties
) {
    private val logger = KotlinLogging.logger {}

    /**
     * Fetches prices for all relevant issues of a coin based on its available data.
     *
     * Selection Strategy:
     * - Filters issues by year (mandatory)
     * - If coin has mintMark → uses only matching mintLetter issues
     * - If coin has NO mintMark → uses ALL mint variations for that year (up to configured limit)
     *
     * Storage Strategy:
     * - If coin has grade → stores only that grade's prices
     * - If coin has NO grade → stores all grades' prices
     *
     * Implements once-per-day update limit by checking if prices were already fetched today.
     *
     * @param coinId The UUID of the coin
     * @param processedIssueIds Set of issue IDs already processed in current run (for deduplication)
     * @return true if prices were successfully fetched and saved for at least one issue, false otherwise
     * @throws RateLimitException if Numista API rate limit is exceeded
     */
    @Transactional
    fun fetchPricesForCoin(coinId: UUID, processedIssueIds: MutableSet<UUID> = mutableSetOf()): Boolean {
        try {
            val coin = coinRepositoryAdapter.findById(coinId)
            if (coin == null || coin.numistaId == null) {
                logger.warn { "Coin $coinId not found or has no Numista ID" }
                return false
            }

            // Get all issues for this coin type
            val issues = issueRepositoryAdapter.findIssuesByNumistaId(coin.numistaId)
            if (issues.isEmpty()) {
                logger.info { "No issues found for coin $coinId (type: ${coin.numistaId})" }
                return false
            }

            // Select all relevant matching issues (up to configured limit)
            val selectedIssues = selectRelevantIssues(
                coin,
                issues,
                maxIssues = issuePriceProperties.maxIssuesPerCoin
            )
            if (selectedIssues.isEmpty()) {
                logger.warn { "Could not select any matching issues for coin $coinId" }
                return false
            }

            logger.info { "Selected ${selectedIssues.size} relevant issue(s) for coin $coinId" }

            var anySuccess = false

            // Fetch prices for each selected issue
            selectedIssues.forEach { issue ->
                if (issue.id == null) {
                    logger.warn { "Issue has no ID, skipping" }
                    return@forEach
                }

                // Skip if already processed in this run
                if (issue.id in processedIssueIds) {
                    logger.debug { "Issue ${issue.id} already processed in this run, skipping" }
                    return@forEach
                }

                // Check if prices already fetched today (once-per-day limit)
                if (!shouldFetchPrices(issue.id)) {
                    logger.debug { "Prices for issue ${issue.id} already fetched today, skipping" }
                    processedIssueIds.add(issue.id)
                    return@forEach
                }

                // Fetch prices from Numista
                val pricesResponse = try {
                    numistaClient.getPricesByIssue(
                        typeId = coin.numistaId,
                        issueId = issue.numistaId
                    )
                } catch (e: FeignException.NotFound) {
                    logger.warn { "No prices available for issue ${issue.id} (Numista ID: ${issue.numistaId})" }
                    processedIssueIds.add(issue.id)
                    return@forEach
                } catch (e: FeignException.TooManyRequests) {
                    logger.error { "Rate limited by Numista API for coin $coinId" }
                    throw RateLimitException("Numista API rate limit exceeded", e)
                }

                // Convert prices, filtering by coin's grade if it has one
                val issuePrices = pricesResponse.prices.mapNotNull { priceByGrade ->
                    val grade = CoinGrade.fromNumistaGrade(priceByGrade.grade)
                    if (grade == null) {
                        logger.debug { "Unknown grade '${priceByGrade.grade}' for issue ${issue.id}, skipping" }
                        return@mapNotNull null
                    }

                    // If coin has a specific grade, only save prices for that grade
                    // If coin has no grade, save all grades for broader coverage
                    if (coin.grade != null && grade != coin.grade) {
                        logger.debug { "Skipping grade $grade for coin $coinId (coin grade: ${coin.grade})" }
                        return@mapNotNull null
                    }

                    IssuePrice(
                        issueId = issue.id.toString(),
                        grade = grade,
                        price = BigDecimal.valueOf(priceByGrade.price),
                        currency = Currency.getInstance(pricesResponse.currency),
                        createdAt = ZonedDateTime.now()
                    )
                }

                if (issuePrices.isEmpty()) {
                    logger.info { "No valid prices returned for issue ${issue.id} matching coin ${coin.id} criteria" }
                    processedIssueIds.add(issue.id)
                    return@forEach
                }

                issuePriceRepositoryAdapter.saveAll(issuePrices)
                val gradeFilter = if (coin.grade != null) " (grade: ${coin.grade})" else " (all grades)"
                logger.info { "Saved ${issuePrices.size} price(s) for coin $coinId (issue: ${issue.id})$gradeFilter" }
                processedIssueIds.add(issue.id)
                anySuccess = true
            }

            return anySuccess
        } catch (e: RateLimitException) {
            throw e // Propagate to scheduler to stop processing
        } catch (e: Exception) {
            logger.error(e) { "Unexpected error fetching prices for coin $coinId" }
            return false
        }
    }

    /**
     * Selects relevant issues for a coin based on available data.
     *
     * Logic:
     * 1. Year is mandatory - always filter by coin's year
     * 2. MintMark is optional:
     *    - If coin has mintMark → filter to issues with matching mintLetter
     *    - If coin has NO mintMark → use ALL issues for that year (all mint variations)
     * 3. Limit to maxIssues to prevent excessive API calls
     *
     * This ensures we get prices for all relevant variations when the coin data is incomplete.
     * For example, a coin from 1943 with no mint mark will fetch prices for 1943, 1943-D, 1943-S, etc.
     *
     * @param coin The coin to match
     * @param issues The available issues for the coin type
     * @param maxIssues Maximum number of issues to return
     * @return List of matching issues (empty if no issues available)
     */
    private fun selectRelevantIssues(coin: Coin, issues: List<Issue>, maxIssues: Int): List<Issue> {
        if (issues.isEmpty()) return emptyList()

        val coinYear = coin.yearOfMinting.year
        val coinMintMark = coin.mintMark?.value

        // Filter by year (mandatory)
        val yearMatches = issues.filter { issue -> issue.year == coinYear }

        if (yearMatches.isEmpty()) {
            logger.warn { "No issues found for coin ${coin.id} with year $coinYear" }
            return emptyList()
        }

        // Further filter by mint mark if coin has one
        val relevantIssues = if (coinMintMark != null) {
            val mintMatches = yearMatches.filter { issue -> issue.mintLetter == coinMintMark }
            if (mintMatches.isNotEmpty()) {
                logger.debug { "Found ${mintMatches.size} issue(s) for coin ${coin.id} (year=$coinYear, mint=$coinMintMark)" }
                mintMatches
            } else {
                logger.info { "No issues found with mint mark '$coinMintMark' for coin ${coin.id}, using all year $coinYear issues" }
                yearMatches
            }
        } else {
            logger.debug { "Coin ${coin.id} has no mint mark, selecting all ${yearMatches.size} issue(s) for year $coinYear" }
            yearMatches
        }

        // Apply limit and warn if exceeded
        val result = relevantIssues.take(maxIssues)
        if (relevantIssues.size > maxIssues) {
            logger.warn {
                "Coin ${coin.id} has ${relevantIssues.size} relevant issues (year=$coinYear, mint=$coinMintMark), " +
                "limited to $maxIssues. Consider increasing max-issues-per-coin if needed."
            }
        }

        return result
    }

    /**
     * Checks if prices should be fetched for an issue.
     * Implements the once-per-day update limit by checking if prices were already fetched today.
     *
     * @param issueId The UUID of the issue
     * @return true if prices should be fetched, false if already fetched today
     */
    private fun shouldFetchPrices(issueId: UUID): Boolean {
        val today = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
        return !issuePriceRepositoryAdapter.hasRecentPrices(issueId, today)
    }
}
