package org.coffer.coffer2.domain

import java.time.Duration
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

enum class ValuationTimeframe(
    val code: String,
    val lookbackDuration: Duration?,
    val bucketInterval: Duration
) {
    // 1 day lookback, hourly buckets
    DAY_1("1d", Duration.ofDays(1), Duration.ofHours(1)),

    // 1 week lookback, 4-hour buckets
    WEEK_1("1w", Duration.ofDays(7), Duration.ofHours(4)),

    // 1 month lookback, daily buckets
    MONTH_1("1m", Duration.ofDays(30), Duration.ofDays(1)),

    // 1 year lookback, daily buckets
    YEAR_1("1y", Duration.ofDays(365), Duration.ofDays(1)),

    // All available data, weekly buckets
    MAX("max", null, Duration.ofDays(7));

    fun getStartTime(now: ZonedDateTime): ZonedDateTime? =
        lookbackDuration?.let { now.minus(it) }

    fun truncateToBucket(time: ZonedDateTime): ZonedDateTime {
        return when {
            bucketInterval >= Duration.ofDays(7) -> {
                // Weekly: truncate to start of week (Monday)
                time.truncatedTo(ChronoUnit.DAYS)
                    .minusDays(time.dayOfWeek.value.toLong() - 1)
            }
            bucketInterval >= Duration.ofDays(1) -> {
                // Daily: truncate to start of day
                time.truncatedTo(ChronoUnit.DAYS)
            }
            bucketInterval >= Duration.ofHours(1) -> {
                // Hourly or multi-hour: truncate to hour boundary
                val hours = bucketInterval.toHours().toInt()
                val truncatedHour = (time.hour / hours) * hours
                time.truncatedTo(ChronoUnit.DAYS).plusHours(truncatedHour.toLong())
            }
            else -> {
                // Minutes: truncate to minute boundary
                val minutes = bucketInterval.toMinutes().toInt()
                val truncatedMinute = (time.minute / minutes) * minutes
                time.truncatedTo(ChronoUnit.HOURS).plusMinutes(truncatedMinute.toLong())
            }
        }
    }

    companion object {
        fun fromCode(code: String): ValuationTimeframe =
            entries.find { it.code.equals(code, ignoreCase = true) }
                ?: throw IllegalArgumentException("Unknown timeframe: $code. Valid values: ${entries.map { it.code }}")
    }
}

fun <T> bucketByInterval(
    items: List<T>,
    timeframe: ValuationTimeframe,
    timestampSelector: (T) -> ZonedDateTime
): List<Pair<ZonedDateTime, List<T>>> {
    if (items.isEmpty()) return emptyList()

    val grouped = items.groupBy { item ->
        timeframe.truncateToBucket(timestampSelector(item))
    }

    return grouped.entries
        .sortedBy { it.key }
        .map { it.key to it.value }
}
