package org.coffer.coffer2.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for issue price fetching scheduler.
 */
@ConfigurationProperties(prefix = "coffer.issue-prices")
data class IssuePriceProperties(
    /**
     * Cron expression for price update schedule.
     * Default: "0 0 2 * * *" (daily at 2 AM)
     */
    val updateIntervalCron: String = "0 0 2 * * *",

    /**
     * Batch size for processing coins before introducing a delay.
     * Default: 50 coins
     */
    val batchSize: Int = 50,

    /**
     * Delay in milliseconds between processing batches.
     * Used for rate limiting to avoid overwhelming the Numista API.
     * Default: 100ms
     */
    val rateLimitDelayMs: Long = 100,

    /**
     * Maximum number of issues to fetch prices for per coin.
     * When no exact match is found, multiple relevant issues may be selected.
     * This limit prevents excessive API calls.
     * Default: 10 issues
     */
    val maxIssuesPerCoin: Int = 10
)
