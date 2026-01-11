package org.coffer.coffer2.repository

import org.coffer.coffer2.IntegrationTestBase
import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.coin.CoinGrade
import org.coffer.coffer2.domain.coin.CoinShape
import org.coffer.coffer2.domain.coin.CoinType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.*

class IssueRepositoryTest : IntegrationTestBase() {

    @Autowired
    private lateinit var issueRepository: IssueRepository

    @Autowired
    private lateinit var coinRepository: CoinRepository

    @Test
    fun `should save and retrieve issue entity`() {
        // Given
        val coin = createTestCoin()
        val savedCoin = coinRepository.save(coin)

        val issue = IssueEntity(
            id = UUID.randomUUID(),
            numistaId = "12345",
            coinId = savedCoin.id,
            year = 2023,
            mintage = 100000L,
            mintLetter = "W",
            comment = "Test issue",
            isProof = true,
            createdAt = ZonedDateTime.now()
        )

        // When
        val savedIssue = issueRepository.save(issue)
        val retrievedIssue = issueRepository.findById(savedIssue.id)

        // Then
        assertTrue(retrievedIssue.isPresent)
        assertEquals(issue.numistaId, retrievedIssue.get().numistaId)
        assertEquals(savedCoin.id, retrievedIssue.get().coinId)
        assertEquals(issue.year, retrievedIssue.get().year)
        assertEquals(issue.mintage, retrievedIssue.get().mintage)
        assertTrue(retrievedIssue.get().isProof)
    }

    @Test
    fun `should cascade delete issues when coin is deleted`() {
        // Given
        val coin = createTestCoin()
        val savedCoin = coinRepository.save(coin)

        val issue = createTestIssue(savedCoin.id)
        val savedIssue = issueRepository.save(issue)

        // When
        coinRepository.deleteById(savedCoin.id)
        coinRepository.flush()
        val retrievedIssue = issueRepository.findById(savedIssue.id)

        // Then
        assertTrue(retrievedIssue.isEmpty)
    }

    @Test
    fun `should fail to save issue with non-existent coin id`() {
        // Given
        val nonExistentCoinId = UUID.randomUUID()
        val issue = createTestIssue(nonExistentCoinId)

        // When & Then
        assertFailsWith<DataIntegrityViolationException> {
            issueRepository.save(issue)
            issueRepository.flush()
        }
    }

    @Test
    fun `should update existing issue`() {
        // Given
        val coin = createTestCoin()
        val savedCoin = coinRepository.save(coin)

        val issue = createTestIssue(savedCoin.id)
        val savedIssue = issueRepository.save(issue)

        // When
        val updatedIssue = savedIssue.copy(
            mintage = 200000L,
            comment = "Updated comment",
            isProof = false
        )
        issueRepository.save(updatedIssue)
        val retrievedIssue = issueRepository.findById(savedIssue.id)

        // Then
        assertTrue(retrievedIssue.isPresent)
        assertEquals(200000L, retrievedIssue.get().mintage)
        assertEquals("Updated comment", retrievedIssue.get().comment)
        assertFalse(retrievedIssue.get().isProof)
    }

    @Test
    fun `should delete issue entity`() {
        // Given
        val coin = createTestCoin()
        val savedCoin = coinRepository.save(coin)

        val issue = createTestIssue(savedCoin.id)
        val savedIssue = issueRepository.save(issue)

        // When
        issueRepository.deleteById(savedIssue.id)
        val retrievedIssue = issueRepository.findById(savedIssue.id)

        // Then
        assertTrue(retrievedIssue.isEmpty)
    }

    @Test
    fun `should save issue with nullable fields`() {
        // Given
        val coin = createTestCoin()
        val savedCoin = coinRepository.save(coin)

        val issue = IssueEntity(
            id = UUID.randomUUID(),
            numistaId = "67890",
            coinId = savedCoin.id,
            year = null,
            mintage = null,
            mintLetter = null,
            comment = null,
            isProof = false,
            createdAt = ZonedDateTime.now()
        )

        // When
        val savedIssue = issueRepository.save(issue)
        val retrievedIssue = issueRepository.findById(savedIssue.id)

        // Then
        assertTrue(retrievedIssue.isPresent)
        assertNull(retrievedIssue.get().year)
        assertNull(retrievedIssue.get().mintage)
        assertNull(retrievedIssue.get().mintLetter)
        assertNull(retrievedIssue.get().comment)
        assertFalse(retrievedIssue.get().isProof)
    }

    @Test
    fun `should save multiple issues for same coin`() {
        // Given
        val coin = createTestCoin()
        val savedCoin = coinRepository.save(coin)

        val issue1 = createTestIssue(savedCoin.id).copy(numistaId = "11111", year = 2020)
        val issue2 = createTestIssue(savedCoin.id).copy(numistaId = "22222", year = 2021)
        val issue3 = createTestIssue(savedCoin.id).copy(numistaId = "33333", year = 2022)

        // When
        issueRepository.saveAll(listOf(issue1, issue2, issue3))
        val allIssues = issueRepository.findAll()

        // Then
        val savedIssues = allIssues.filter { it.coinId == savedCoin.id }
        assertTrue(savedIssues.size >= 3)
    }

    private fun createTestCoin(): CoinEntity = CoinEntity(
        id = UUID.randomUUID(),
        title = "Test Coin for Issue",
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
        numistaId = "TEST123",
        coinId = coinId,
        year = 2023,
        mintage = 50000L,
        mintLetter = "D",
        comment = "Test comment",
        isProof = false,
        createdAt = ZonedDateTime.now()
    )
}
