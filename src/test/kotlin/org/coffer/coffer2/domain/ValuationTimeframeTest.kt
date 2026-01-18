package org.coffer.coffer2.domain

import java.time.Duration
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ValuationTimeframeTest {

    private val zone = ZoneId.of("UTC")

    // ==================== fromCode tests ====================

    @Test
    fun `fromCode should parse valid codes case-insensitively`() {
        assertEquals(ValuationTimeframe.HOUR_1, ValuationTimeframe.fromCode("1h"))
        assertEquals(ValuationTimeframe.HOUR_1, ValuationTimeframe.fromCode("1H"))
        assertEquals(ValuationTimeframe.DAY_1, ValuationTimeframe.fromCode("1d"))
        assertEquals(ValuationTimeframe.WEEK_1, ValuationTimeframe.fromCode("1w"))
        assertEquals(ValuationTimeframe.MONTH_1, ValuationTimeframe.fromCode("1m"))
        assertEquals(ValuationTimeframe.YEAR_1, ValuationTimeframe.fromCode("1y"))
        assertEquals(ValuationTimeframe.MAX, ValuationTimeframe.fromCode("max"))
        assertEquals(ValuationTimeframe.MAX, ValuationTimeframe.fromCode("MAX"))
    }

    @Test
    fun `fromCode should throw for invalid code`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            ValuationTimeframe.fromCode("invalid")
        }
        assertEquals(
            "Unknown timeframe: invalid. Valid values: [1h, 1d, 1w, 1m, 1y, max]",
            exception.message
        )
    }

    // ==================== getStartTime tests ====================

    @Test
    fun `getStartTime should return correct start time for each timeframe`() {
        val now = ZonedDateTime.of(2025, 1, 15, 12, 30, 0, 0, zone)

        assertEquals(
            ZonedDateTime.of(2025, 1, 15, 11, 30, 0, 0, zone),
            ValuationTimeframe.HOUR_1.getStartTime(now)
        )
        assertEquals(
            ZonedDateTime.of(2025, 1, 14, 12, 30, 0, 0, zone),
            ValuationTimeframe.DAY_1.getStartTime(now)
        )
        assertEquals(
            ZonedDateTime.of(2025, 1, 8, 12, 30, 0, 0, zone),
            ValuationTimeframe.WEEK_1.getStartTime(now)
        )
    }

    @Test
    fun `getStartTime should return null for MAX timeframe`() {
        val now = ZonedDateTime.now(zone)
        assertNull(ValuationTimeframe.MAX.getStartTime(now))
    }

    // ==================== truncateToBucket tests - 5 minute intervals ====================

    @Test
    fun `truncateToBucket should truncate to 5-minute boundaries for HOUR_1`() {
        val time = ZonedDateTime.of(2025, 1, 15, 14, 37, 45, 123456789, zone)

        val result = ValuationTimeframe.HOUR_1.truncateToBucket(time)

        assertEquals(ZonedDateTime.of(2025, 1, 15, 14, 35, 0, 0, zone), result)
    }

    @Test
    fun `truncateToBucket should handle exact 5-minute boundary for HOUR_1`() {
        val time = ZonedDateTime.of(2025, 1, 15, 14, 35, 0, 0, zone)

        val result = ValuationTimeframe.HOUR_1.truncateToBucket(time)

        assertEquals(ZonedDateTime.of(2025, 1, 15, 14, 35, 0, 0, zone), result)
    }

    @Test
    fun `truncateToBucket should truncate minutes 0-4 to 0 for DAY_1`() {
        val time = ZonedDateTime.of(2025, 1, 15, 10, 3, 30, 0, zone)

        val result = ValuationTimeframe.DAY_1.truncateToBucket(time)

        assertEquals(ZonedDateTime.of(2025, 1, 15, 10, 0, 0, 0, zone), result)
    }

    @Test
    fun `truncateToBucket should truncate minutes 55-59 to 55 for DAY_1`() {
        val time = ZonedDateTime.of(2025, 1, 15, 10, 58, 30, 0, zone)

        val result = ValuationTimeframe.DAY_1.truncateToBucket(time)

        assertEquals(ZonedDateTime.of(2025, 1, 15, 10, 55, 0, 0, zone), result)
    }

    // ==================== truncateToBucket tests - hourly intervals ====================

    @Test
    fun `truncateToBucket should truncate to hour boundary for WEEK_1`() {
        val time = ZonedDateTime.of(2025, 1, 15, 14, 37, 45, 0, zone)

        val result = ValuationTimeframe.WEEK_1.truncateToBucket(time)

        assertEquals(ZonedDateTime.of(2025, 1, 15, 14, 0, 0, 0, zone), result)
    }

    @Test
    fun `truncateToBucket should handle exact hour for WEEK_1`() {
        val time = ZonedDateTime.of(2025, 1, 15, 14, 0, 0, 0, zone)

        val result = ValuationTimeframe.WEEK_1.truncateToBucket(time)

        assertEquals(ZonedDateTime.of(2025, 1, 15, 14, 0, 0, 0, zone), result)
    }

    // ==================== truncateToBucket tests - 4-hour intervals ====================

    @Test
    fun `truncateToBucket should truncate to 4-hour boundaries for MONTH_1`() {
        // Hour 14 should truncate to hour 12 (12 is 3*4)
        val time = ZonedDateTime.of(2025, 1, 15, 14, 37, 45, 0, zone)

        val result = ValuationTimeframe.MONTH_1.truncateToBucket(time)

        assertEquals(ZonedDateTime.of(2025, 1, 15, 12, 0, 0, 0, zone), result)
    }

    @Test
    fun `truncateToBucket should truncate hours 0-3 to 0 for MONTH_1`() {
        val time = ZonedDateTime.of(2025, 1, 15, 3, 30, 0, 0, zone)

        val result = ValuationTimeframe.MONTH_1.truncateToBucket(time)

        assertEquals(ZonedDateTime.of(2025, 1, 15, 0, 0, 0, 0, zone), result)
    }

    @Test
    fun `truncateToBucket should truncate hours 20-23 to 20 for MONTH_1`() {
        val time = ZonedDateTime.of(2025, 1, 15, 22, 30, 0, 0, zone)

        val result = ValuationTimeframe.MONTH_1.truncateToBucket(time)

        assertEquals(ZonedDateTime.of(2025, 1, 15, 20, 0, 0, 0, zone), result)
    }

    // ==================== truncateToBucket tests - daily intervals ====================

    @Test
    fun `truncateToBucket should truncate to start of day for YEAR_1`() {
        val time = ZonedDateTime.of(2025, 1, 15, 14, 37, 45, 123456789, zone)

        val result = ValuationTimeframe.YEAR_1.truncateToBucket(time)

        assertEquals(ZonedDateTime.of(2025, 1, 15, 0, 0, 0, 0, zone), result)
    }

    @Test
    fun `truncateToBucket should handle midnight for YEAR_1`() {
        val time = ZonedDateTime.of(2025, 1, 15, 0, 0, 0, 0, zone)

        val result = ValuationTimeframe.YEAR_1.truncateToBucket(time)

        assertEquals(ZonedDateTime.of(2025, 1, 15, 0, 0, 0, 0, zone), result)
    }

    // ==================== truncateToBucket tests - weekly intervals ====================

    @Test
    fun `truncateToBucket should truncate to Monday for MAX timeframe`() {
        // Wednesday Jan 15, 2025 should truncate to Monday Jan 13, 2025
        val wednesday = ZonedDateTime.of(2025, 1, 15, 14, 37, 45, 0, zone)

        val result = ValuationTimeframe.MAX.truncateToBucket(wednesday)

        assertEquals(ZonedDateTime.of(2025, 1, 13, 0, 0, 0, 0, zone), result)
    }

    @Test
    fun `truncateToBucket should keep Monday as Monday for MAX timeframe`() {
        val monday = ZonedDateTime.of(2025, 1, 13, 10, 30, 0, 0, zone)

        val result = ValuationTimeframe.MAX.truncateToBucket(monday)

        assertEquals(ZonedDateTime.of(2025, 1, 13, 0, 0, 0, 0, zone), result)
    }

    @Test
    fun `truncateToBucket should truncate Sunday to previous Monday for MAX timeframe`() {
        // Sunday Jan 19, 2025 should truncate to Monday Jan 13, 2025
        val sunday = ZonedDateTime.of(2025, 1, 19, 23, 59, 59, 0, zone)

        val result = ValuationTimeframe.MAX.truncateToBucket(sunday)

        assertEquals(ZonedDateTime.of(2025, 1, 13, 0, 0, 0, 0, zone), result)
    }

    @Test
    fun `truncateToBucket should handle Friday correctly for MAX timeframe`() {
        // Friday Jan 17, 2025 should truncate to Monday Jan 13, 2025
        val friday = ZonedDateTime.of(2025, 1, 17, 16, 0, 0, 0, zone)

        val result = ValuationTimeframe.MAX.truncateToBucket(friday)

        assertEquals(ZonedDateTime.of(2025, 1, 13, 0, 0, 0, 0, zone), result)
    }

    // ==================== Configuration tests ====================

    @Test
    fun `all timeframes should have correct bucket intervals`() {
        assertEquals(Duration.ofMinutes(5), ValuationTimeframe.HOUR_1.bucketInterval)
        assertEquals(Duration.ofMinutes(5), ValuationTimeframe.DAY_1.bucketInterval)
        assertEquals(Duration.ofHours(1), ValuationTimeframe.WEEK_1.bucketInterval)
        assertEquals(Duration.ofHours(4), ValuationTimeframe.MONTH_1.bucketInterval)
        assertEquals(Duration.ofDays(1), ValuationTimeframe.YEAR_1.bucketInterval)
        assertEquals(Duration.ofDays(7), ValuationTimeframe.MAX.bucketInterval)
    }

    @Test
    fun `all timeframes except MAX should have lookback duration`() {
        assertEquals(Duration.ofHours(1), ValuationTimeframe.HOUR_1.lookbackDuration)
        assertEquals(Duration.ofDays(1), ValuationTimeframe.DAY_1.lookbackDuration)
        assertEquals(Duration.ofDays(7), ValuationTimeframe.WEEK_1.lookbackDuration)
        assertEquals(Duration.ofDays(30), ValuationTimeframe.MONTH_1.lookbackDuration)
        assertEquals(Duration.ofDays(365), ValuationTimeframe.YEAR_1.lookbackDuration)
        assertNull(ValuationTimeframe.MAX.lookbackDuration)
    }
}
