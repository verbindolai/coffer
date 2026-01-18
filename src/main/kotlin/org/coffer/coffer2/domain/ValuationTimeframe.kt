package org.coffer.coffer2.domain

import java.time.Duration
import java.time.ZonedDateTime

enum class ValuationTimeframe(
    val code: String,
    val duration: Duration?,
    val targetDataPoints: Int
) {
    HOUR_1("1h", Duration.ofHours(1), 12),
    DAY_1("1d", Duration.ofDays(1), 24),
    WEEK_1("1w", Duration.ofDays(7), 28),
    MONTH_1("1m", Duration.ofDays(30), 30),
    YEAR_1("1y", Duration.ofDays(365), 52),
    MAX("max", null, 100);

    fun getStartTime(now: ZonedDateTime): ZonedDateTime? =
        duration?.let { now.minus(it) }

    companion object {
        fun fromCode(code: String): ValuationTimeframe =
            entries.find { it.code.equals(code, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown timeframe: $code. Valid values: ${entries.map { it.code }}")
    }
}
