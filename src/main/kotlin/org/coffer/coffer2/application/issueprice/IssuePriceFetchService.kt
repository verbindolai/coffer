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
import org.coffer.coffer2.remote.numista.NumistaPriceResponse
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
 * Deduplication Strategy:
 * - lastPriceFetchAttempt: Prevents fetching same issue across multiple days (once-per-day)
 * - processedIssueIds: Prevents fetching same issue within a transaction (batch efficiency)
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
     * Fetches prices for all relevant issues of a coin.
     *
     * @param coinId The UUID of the coin
     * @param processedIssueIds Optional set for within-transaction deduplication
     * @return true if processing completed successfully (even if no prices found)
     * @throws RateLimitException if Numista API rate limit is exceeded
     */
    @Transactional
    fun fetchPricesForCoin(coinId: UUID, processedIssueIds: MutableSet<UUID>? = null): Boolean {
        try {
            // Load and validate coin
            val coin = coinRepositoryAdapter.findById(coinId)
            if (coin == null || coin.numistaId == null) {
                logger.warn { "Coin $coinId not found or has no Numista ID" }
                return false
            }

            // Load and select relevant issues
            val issues = issueRepositoryAdapter.findIssuesByNumistaId(coin.numistaId)
            if (issues.isEmpty()) {
                logger.info { "No issues found for coin $coinId (type: ${coin.numistaId})" }
                return false
            }

            val selectedIssues = selectRelevantIssues(coin, issues, issuePriceProperties.maxIssuesPerCoin)
            if (selectedIssues.isEmpty()) {
                logger.warn { "Could not select any matching issues for coin $coinId" }
                return false
            }

            logger.info { "Selected ${selectedIssues.size} relevant issue(s) for coin $coinId" }

            // Process each selected issue
            selectedIssues.forEach { issue ->
                if (shouldSkipIssue(issue, processedIssueIds)) {
                    return@forEach
                }

                try {
                    processIssue(coin, issue)
                    processedIssueIds?.add(issue.id!!)
                } catch (e: RateLimitException) {
                    throw e // Propagate to stop batch processing
                }
            }

            return true
        } catch (e: RateLimitException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Unexpected error fetching prices for coin $coinId" }
            return false
        }
    }

    /**
     * Determines if an issue should be skipped based on:
     * 1. Missing ID
     * 2. Already processed in this transaction
     * 3. Already attempted today
     */
    private fun shouldSkipIssue(issue: Issue, processedIssueIds: Set<UUID>?): Boolean {
        val issueId = issue.id
        if (issueId == null) {
            logger.warn { "Issue has no ID, skipping" }
            return true
        }

        // Skip if already processed in this transaction (within-transaction deduplication)
        if (processedIssueIds?.contains(issueId) == true) {
            logger.debug { "Issue $issueId already processed in this run, skipping" }
            return true
        }

        // Skip if already attempted today (once-per-day limit)
        val lastAttempt = issue.lastPriceFetchAttempt
        if (lastAttempt != null) {
            val today = ZonedDateTime.now().truncatedTo(ChronoUnit.DAYS)
            val lastAttemptDay = lastAttempt.truncatedTo(ChronoUnit.DAYS)
            if (!lastAttemptDay.isBefore(today)) {
                logger.debug { "Issue $issueId already attempted today, skipping" }
                return true
            }
        }

        return false
    }

    /**
     * Processes a single issue: fetch prices, convert, save, and update timestamp.
     * Updates timestamp regardless of outcome (success, 404, or no matching prices).
     */
    private fun processIssue(coin: Coin, issue: Issue) {
        val issueId = issue.id!!
        val pricesResponse = fetchPricesFromApi(coin, issue) ?: return

        val issuePrices = convertAndFilterPrices(coin, issue, pricesResponse)

        if (issuePrices.isNotEmpty()) {
            issuePriceRepositoryAdapter.saveAll(issuePrices)
            val gradeFilter = if (coin.grade != null) " (grade: ${coin.grade})" else " (all grades)"
            logger.info { "Saved ${issuePrices.size} price(s) for coin ${coin.id} (issue: $issueId)$gradeFilter" }
        } else {
            logger.info { "No matching prices for issue $issueId (coin ${coin.id} criteria)" }
        }

        // Always update timestamp (even on 404 or no matches)
        issueRepositoryAdapter.updateLastPriceFetchAttempt(issueId, ZonedDateTime.now())
    }

    /**
     * Fetches prices from Numista API for an issue.
     * Returns null if no prices available (404) or rate limited (throws exception).
     * Updates timestamp on 404 to prevent repeated attempts.
     */
    private fun fetchPricesFromApi(coin: Coin, issue: Issue): NumistaPriceResponse? {
        val issueId = issue.id!!

        return try {
            numistaClient.getPricesByIssue(
                typeId = coin.numistaId!!,
                issueId = issue.numistaId
            )
        } catch (_: FeignException.NotFound) {
            logger.info { "No prices available for issue $issueId (Numista ID: ${issue.numistaId})" }
            // Update timestamp - 404 is a successful check (prices don't exist)
            issueRepositoryAdapter.updateLastPriceFetchAttempt(issueId, ZonedDateTime.now())
            null
        } catch (_: FeignException.TooManyRequests) {
            logger.error { "Rate limited by Numista API" }
            throw RateLimitException("Numista API rate limit exceeded")
        }
    }

    /**
     * Converts API prices to domain objects and filters by coin's grade if specified.
     * If coin has a grade: only returns prices for that grade
     * If coin has no grade: returns prices for all grades
     */
    private fun convertAndFilterPrices(
        coin: Coin,
        issue: Issue,
        pricesResponse: NumistaPriceResponse
    ): List<IssuePrice> {
        return pricesResponse.prices.mapNotNull { priceByGrade ->
            val grade = CoinGrade.fromNumistaGrade(priceByGrade.grade)
            if (grade == null) {
                logger.info { "Unknown grade '${priceByGrade.grade}' for issue ${issue.id}, skipping" }
                return@mapNotNull null
            }

            // If coin has a specific grade, only include prices for that grade
            if (coin.grade != null && grade != coin.grade) {
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
    }

    /**
     * Selects relevant issues for a coin based on year and mint mark matching.
     *
     * Logic:
     * 1. Year is mandatory - always filter by coin's year
     * 2. MintMark is optional:
     *    - If coin has mintMark → filter to issues with matching mintLetter
     *    - If coin has NO mintMark → use ALL issues for that year
     * 3. Limit to maxIssues to prevent excessive API calls
     *
     * Example: A 1943 coin with no mint mark will match 1943, 1943-D, 1943-S, etc.
     */
    private fun selectRelevantIssues(coin: Coin, issues: List<Issue>, maxIssues: Int): List<Issue> {
        if (issues.isEmpty()) return emptyList()

        val coinYear = coin.yearOfMinting.year
        val coinMintMark = coin.mintMark?.value

        // Filter by year (mandatory)
        val yearMatches = issues.filter { it.year == coinYear }
        if (yearMatches.isEmpty()) {
            logger.warn { "No issues found for coin ${coin.id} with year $coinYear" }
            return emptyList()
        }

        // Further filter by mint mark if coin has one
        val relevantIssues = if (coinMintMark != null) {
            val mintMatches = yearMatches.filter { it.mintLetter == coinMintMark }
            if (mintMatches.isNotEmpty()) {
                logger.debug { "Found ${mintMatches.size} issue(s) for coin ${coin.id} (year=$coinYear, mint=$coinMintMark)" }
                mintMatches
            } else {
                logger.info { "No issues with mint mark '$coinMintMark' for coin ${coin.id}, using all year $coinYear issues" }
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
}
