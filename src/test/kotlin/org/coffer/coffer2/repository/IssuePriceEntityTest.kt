package org.coffer.coffer2.repository

import org.coffer.coffer2.domain.coin.CoinGrade
import org.coffer.coffer2.domain.coin.IssuePrice
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class IssuePriceEntityTest {

    @Test
    fun `toIssuePrice should convert entity to domain model correctly`() {
        // Given
        val entityId = UUID.randomUUID()
        val issueId = UUID.randomUUID()
        val createdAt = ZonedDateTime.now()

        val entity = IssuePriceEntity(
            id = entityId,
            issueId = issueId,
            grade = CoinGrade.UNCIRCULATED,
            price = BigDecimal("1500.00"),
            currencyCode = "USD",
            createdAt = createdAt
        )

        // When
        val issuePrice = entity.toIssuePrice()

        // Then
        assertEquals(entityId.toString(), issuePrice.id)
        assertEquals(issueId.toString(), issuePrice.issueId)
        assertEquals(CoinGrade.UNCIRCULATED, issuePrice.grade)
        assertEquals(BigDecimal("1500.00"), issuePrice.price)
        assertEquals(Currency.getInstance("USD"), issuePrice.currency)
        assertEquals(createdAt, issuePrice.createdAt)
    }

    @Test
    fun `fromIssuePrice should convert domain model to entity correctly`() {
        // Given
        val issueId = UUID.randomUUID()
        val createdAt = ZonedDateTime.now()

        val issuePrice = IssuePrice(
            id = UUID.randomUUID().toString(),
            issueId = issueId.toString(),
            grade = CoinGrade.PROOF,
            price = BigDecimal("2500.50"),
            currency = Currency.getInstance("EUR"),
            createdAt = createdAt
        )

        // When
        val entity = IssuePriceEntity.fromIssuePrice(issuePrice)

        // Then
        assertEquals(issuePrice.id, entity.id.toString())
        assertEquals(issueId, entity.issueId)
        assertEquals(CoinGrade.PROOF, entity.grade)
        assertEquals(BigDecimal("2500.50"), entity.price)
        assertEquals("EUR", entity.currencyCode)
        assertEquals(createdAt, entity.createdAt)
    }

    @Test
    fun `fromIssuePrice should generate UUID when price id is null`() {
        // Given
        val issuePrice = IssuePrice(
            id = null,
            issueId = UUID.randomUUID().toString(),
            grade = CoinGrade.VERY_FINE,
            price = BigDecimal("100.00"),
            currency = Currency.getInstance("GBP"),
            createdAt = ZonedDateTime.now()
        )

        // When
        val entity = IssuePriceEntity.fromIssuePrice(issuePrice)

        // Then
        assertNotNull(entity.id)
    }

    @Test
    fun `round-trip conversion should preserve all fields`() {
        // Given
        val issueId = UUID.randomUUID()
        val createdAt = ZonedDateTime.now()

        val originalPrice = IssuePrice(
            id = UUID.randomUUID().toString(),
            issueId = issueId.toString(),
            grade = CoinGrade.EXTREMELY_FINE,
            price = BigDecimal("750.25"),
            currency = Currency.getInstance("CHF"),
            createdAt = createdAt
        )

        // When
        val entity = IssuePriceEntity.fromIssuePrice(originalPrice)
        val resultPrice = entity.toIssuePrice()

        // Then
        assertEquals(originalPrice.id, resultPrice.id)
        assertEquals(originalPrice.issueId, resultPrice.issueId)
        assertEquals(originalPrice.grade, resultPrice.grade)
        assertEquals(originalPrice.price, resultPrice.price)
        assertEquals(originalPrice.currency, resultPrice.currency)
        assertEquals(originalPrice.createdAt, resultPrice.createdAt)
    }

    @Test
    fun `should handle different coin grades correctly`() {
        // Given
        val issueId = UUID.randomUUID()
        val grades = listOf(
            CoinGrade.GOOD,
            CoinGrade.VERY_GOOD,
            CoinGrade.FINE,
            CoinGrade.VERY_FINE,
            CoinGrade.EXTREMELY_FINE,
            CoinGrade.ABOUT_UNCIRCULATED,
            CoinGrade.UNCIRCULATED,
            CoinGrade.PROOF
        )

        // When & Then
        grades.forEach { grade ->
            val issuePrice = IssuePrice(
                id = UUID.randomUUID().toString(),
                issueId = issueId.toString(),
                grade = grade,
                price = BigDecimal("100.00"),
                currency = Currency.getInstance("USD"),
                createdAt = ZonedDateTime.now()
            )

            val entity = IssuePriceEntity.fromIssuePrice(issuePrice)
            val result = entity.toIssuePrice()

            assertEquals(grade, result.grade)
        }
    }

    @Test
    fun `should handle different currencies correctly`() {
        // Given
        val issueId = UUID.randomUUID()
        val currencies = listOf("USD", "EUR", "GBP", "CHF", "JPY", "CAD", "AUD")

        // When & Then
        currencies.forEach { currencyCode ->
            val issuePrice = IssuePrice(
                id = UUID.randomUUID().toString(),
                issueId = issueId.toString(),
                grade = CoinGrade.UNCIRCULATED,
                price = BigDecimal("1000.00"),
                currency = Currency.getInstance(currencyCode),
                createdAt = ZonedDateTime.now()
            )

            val entity = IssuePriceEntity.fromIssuePrice(issuePrice)
            val result = entity.toIssuePrice()

            assertEquals(Currency.getInstance(currencyCode), result.currency)
        }
    }
}
