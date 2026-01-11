package org.coffer.coffer2.repository

import org.coffer.coffer2.IntegrationTestBase
import org.coffer.coffer2.domain.coin.CoinGrade
import org.coffer.coffer2.domain.coin.CoinShape
import org.coffer.coffer2.domain.coin.CoinType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class IssuePriceRepositoryTest : IntegrationTestBase() {

    @Autowired
    private lateinit var issuePriceRepository: IssuePriceRepository

    @Autowired
    private lateinit var issueRepository: IssueRepository

    @Autowired
    private lateinit var coinRepository: CoinRepository

    @Test
    fun `should save and retrieve issue price entity`() {
        // Given
        val coin = createTestCoin()
        val savedCoin = coinRepository.save(coin)
        val issue = createTestIssue(savedCoin.id)
        val savedIssue = issueRepository.save(issue)

        val issuePrice = IssuePriceEntity(
            id = UUID.randomUUID(),
            issueId = savedIssue.id,
            grade = CoinGrade.UNCIRCULATED,
            price = BigDecimal("1500.00"),
            currencyCode = "USD",
            createdAt = ZonedDateTime.now()
        )

        // When
        val savedPrice = issuePriceRepository.save(issuePrice)
        val retrievedPrice = issuePriceRepository.findById(savedPrice.id)

        // Then
        assertTrue(retrievedPrice.isPresent)
        assertEquals(savedIssue.id, retrievedPrice.get().issueId)
        assertEquals(CoinGrade.UNCIRCULATED, retrievedPrice.get().grade)
        assertEquals(0, BigDecimal("1500.00").compareTo(retrievedPrice.get().price))
        assertEquals("USD", retrievedPrice.get().currencyCode)
    }

    @Test
    fun `should cascade delete issue prices when issue is deleted`() {
        // Given
        val coin = createTestCoin()
        val savedCoin = coinRepository.save(coin)
        val issue = createTestIssue(savedCoin.id)
        val savedIssue = issueRepository.save(issue)

        val issuePrice = createTestIssuePrice(savedIssue.id)
        val savedPrice = issuePriceRepository.save(issuePrice)

        // When
        issueRepository.deleteById(savedIssue.id)
        issueRepository.flush()
        val retrievedPrice = issuePriceRepository.findById(savedPrice.id)

        // Then
        assertTrue(retrievedPrice.isEmpty)
    }

    @Test
    fun `should fail to save issue price with non-existent issue id`() {
        // Given
        val nonExistentIssueId = UUID.randomUUID()
        val issuePrice = createTestIssuePrice(nonExistentIssueId)

        // When & Then
        assertFailsWith<DataIntegrityViolationException> {
            issuePriceRepository.save(issuePrice)
            issuePriceRepository.flush()
        }
    }

    @Test
    fun `should update existing issue price`() {
        // Given
        val coin = createTestCoin()
        val savedCoin = coinRepository.save(coin)
        val issue = createTestIssue(savedCoin.id)
        val savedIssue = issueRepository.save(issue)

        val issuePrice = createTestIssuePrice(savedIssue.id)
        val savedPrice = issuePriceRepository.save(issuePrice)

        // When
        val updatedPrice = savedPrice.copy(
            price = BigDecimal("2000.00"),
            grade = CoinGrade.PROOF
        )
        issuePriceRepository.save(updatedPrice)
        val retrievedPrice = issuePriceRepository.findById(savedPrice.id)

        // Then
        assertTrue(retrievedPrice.isPresent)
        assertEquals(0, BigDecimal("2000.00").compareTo(retrievedPrice.get().price))
        assertEquals(CoinGrade.PROOF, retrievedPrice.get().grade)
    }

    @Test
    fun `should save multiple prices for same issue with different grades`() {
        // Given
        val coin = createTestCoin()
        val savedCoin = coinRepository.save(coin)
        val issue = createTestIssue(savedCoin.id)
        val savedIssue = issueRepository.save(issue)

        val price1 = createTestIssuePrice(savedIssue.id).copy(
            grade = CoinGrade.FINE,
            price = BigDecimal("100.00")
        )
        val price2 = createTestIssuePrice(savedIssue.id).copy(
            grade = CoinGrade.VERY_FINE,
            price = BigDecimal("200.00")
        )
        val price3 = createTestIssuePrice(savedIssue.id).copy(
            grade = CoinGrade.UNCIRCULATED,
            price = BigDecimal("500.00")
        )

        // When
        issuePriceRepository.saveAll(listOf(price1, price2, price3))
        val allPrices = issuePriceRepository.findAll()

        // Then
        val savedPrices = allPrices.filter { it.issueId == savedIssue.id }
        assertTrue(savedPrices.size >= 3)
    }

    @Test
    fun `should handle different coin grades`() {
        // Given
        val coin = createTestCoin()
        val savedCoin = coinRepository.save(coin)
        val issue = createTestIssue(savedCoin.id)
        val savedIssue = issueRepository.save(issue)

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
            val price = createTestIssuePrice(savedIssue.id).copy(grade = grade)
            val savedPrice = issuePriceRepository.save(price)
            val retrieved = issuePriceRepository.findById(savedPrice.id)
            assertTrue(retrieved.isPresent)
            assertEquals(grade, retrieved.get().grade)
        }
    }

    @Test
    fun `should handle different currencies`() {
        // Given
        val coin = createTestCoin()
        val savedCoin = coinRepository.save(coin)
        val issue = createTestIssue(savedCoin.id)
        val savedIssue = issueRepository.save(issue)

        val currencies = listOf("USD", "EUR", "GBP", "CHF", "JPY")

        // When & Then
        currencies.forEach { currency ->
            val price = createTestIssuePrice(savedIssue.id).copy(currencyCode = currency)
            val savedPrice = issuePriceRepository.save(price)
            val retrieved = issuePriceRepository.findById(savedPrice.id)
            assertTrue(retrieved.isPresent)
            assertEquals(currency, retrieved.get().currencyCode)
        }
    }

    private fun createTestCoin(): CoinEntity = CoinEntity(
        id = UUID.randomUUID(),
        title = "Test Coin for Price",
        denomination = BigDecimal("1.00"),
        currencyCode = "USD",
        yearOfMinting = 2023,
        issuerCountryCode = "US",
        mintMark = null,
        grade = CoinGrade.UNCIRCULATED,
        type = CoinType.STANDARD_CIRCULATION,
        notes = null,
        numistaId = null,
        shape = CoinShape.CIRCULAR,
        weightInGrams = BigDecimal("5.00"),
        purity = null,
        metalType = null,
        rarityScore = null,
        createdAt = ZonedDateTime.now(),
        diameterInMillimeters = null,
        thicknessInMillimeters = null,
        lastPriceUpdate = null
    )

    private fun createTestIssue(coinId: UUID): IssueEntity = IssueEntity(
        id = UUID.randomUUID(),
        numistaId = "PRICE_TEST",
        coinId = coinId,
        year = 2023,
        mintage = 50000L,
        mintLetter = null,
        comment = null,
        isProof = false,
        createdAt = ZonedDateTime.now()
    )

    private fun createTestIssuePrice(issueId: UUID): IssuePriceEntity = IssuePriceEntity(
        id = UUID.randomUUID(),
        issueId = issueId,
        grade = CoinGrade.UNCIRCULATED,
        price = BigDecimal("1000.00"),
        currencyCode = "USD",
        createdAt = ZonedDateTime.now()
    )
}
