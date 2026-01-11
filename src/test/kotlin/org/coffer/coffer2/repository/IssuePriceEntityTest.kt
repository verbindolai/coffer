package org.coffer.coffer2.repository

import org.coffer.coffer2.domain.coin.CoinGrade
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.assertEquals

class IssuePriceEntityTest {

    @Test
    fun `should convert entity to domain and back without losing data`() {
        // Given - entity with all fields populated
        val originalEntity = IssuePriceEntity(
            id = UUID.randomUUID(),
            issueId = UUID.randomUUID(),
            grade = CoinGrade.UNCIRCULATED,
            price = BigDecimal("1500.00"),
            currencyCode = "USD",
            createdAt = ZonedDateTime.now()
        )

        // When - convert to domain and back
        val issuePrice = originalEntity.toIssuePrice()
        val roundTripEntity = IssuePriceEntity.fromIssuePrice(issuePrice)

        // Then - verify key fields match
        assertEquals(originalEntity.id, UUID.fromString(roundTripEntity.id.toString()))
        assertEquals(originalEntity.issueId, UUID.fromString(roundTripEntity.issueId.toString()))
        assertEquals(originalEntity.grade, roundTripEntity.grade)
        assertEquals(0, originalEntity.price.compareTo(roundTripEntity.price))
        assertEquals(originalEntity.currencyCode, roundTripEntity.currencyCode)
    }
}
