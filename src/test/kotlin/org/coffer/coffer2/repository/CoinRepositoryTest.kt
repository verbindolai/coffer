package org.coffer.coffer2.repository

import org.coffer.coffer2.IntegrationTestBase
import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.coin.CoinGrade
import org.coffer.coffer2.domain.coin.CoinShape
import org.coffer.coffer2.domain.coin.CoinType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoinRepositoryTest : IntegrationTestBase() {

    @Autowired
    private lateinit var coinRepository: CoinRepository

    @Test
    fun `should save and retrieve coin entity`() {
        // Given
        val coin = CoinEntity(
            id = UUID.randomUUID(),
            title = "Test Gold Coin",
            denomination = BigDecimal("50.00"),
            currencyCode = "USD",
            yearOfMinting = 2023,
            issuerCountryCode = "US",
            mintMark = "W",
            grade = CoinGrade.UNCIRCULATED,
            type = CoinType.BULLION,
            notes = "Integration test coin",
            numistaId = "TEST123",
            shape = CoinShape.CIRCULAR,
            weightInGrams = BigDecimal("31.10"),
            purity = BigDecimal("999.9"),
            metalType = MetalType.GOLD,
            rarityScore = 50,
            createdAt = ZonedDateTime.now(),
            diameterInMillimeters = BigDecimal("32.70"),
            thicknessInMillimeters = BigDecimal("2.87"),
            lastPriceUpdate = null
        )

        // When
        val savedCoin = coinRepository.save(coin)
        val retrievedCoin = coinRepository.findById(savedCoin.id)

        // Then
        assertTrue(retrievedCoin.isPresent)
        assertEquals(coin.title, retrievedCoin.get().title)
        assertEquals(0, coin.denomination!!.compareTo(retrievedCoin.get().denomination))
        assertEquals(coin.currencyCode, retrievedCoin.get().currencyCode)
        assertEquals(coin.yearOfMinting, retrievedCoin.get().yearOfMinting)
        assertEquals(coin.metalType, retrievedCoin.get().metalType)
    }

    @Test
    fun `should update existing coin entity`() {
        // Given
        val coin = CoinEntity(
            id = UUID.randomUUID(),
            title = "Original Title",
            denomination = BigDecimal("10.00"),
            currencyCode = "EUR",
            yearOfMinting = 2022,
            issuerCountryCode = "FR",
            mintMark = null,
            grade = CoinGrade.FINE,
            type = CoinType.STANDARD_CIRCULATION,
            notes = null,
            numistaId = null,
            shape = CoinShape.CIRCULAR,
            weightInGrams = BigDecimal("8.50"),
            purity = null,
            metalType = null,
            rarityScore = null,
            createdAt = ZonedDateTime.now(),
            diameterInMillimeters = null,
            thicknessInMillimeters = null,
            lastPriceUpdate = null
        )
        val savedCoin = coinRepository.save(coin)

        // When
        val updatedCoin = savedCoin.copy(title = "Updated Title", notes = "Updated notes")
        coinRepository.save(updatedCoin)
        val retrievedCoin = coinRepository.findById(savedCoin.id)

        // Then
        assertTrue(retrievedCoin.isPresent)
        assertEquals("Updated Title", retrievedCoin.get().title)
        assertEquals("Updated notes", retrievedCoin.get().notes)
    }

    @Test
    fun `should delete coin entity`() {
        // Given
        val coin = CoinEntity(
            id = UUID.randomUUID(),
            title = "Coin to Delete",
            denomination = null,
            currencyCode = "GBP",
            yearOfMinting = 2021,
            issuerCountryCode = "GB",
            mintMark = null,
            grade = null,
            type = CoinType.OTHER,
            notes = null,
            numistaId = null,
            shape = CoinShape.UNKNOWN,
            weightInGrams = BigDecimal("5.00"),
            purity = null,
            metalType = null,
            rarityScore = null,
            createdAt = ZonedDateTime.now(),
            diameterInMillimeters = null,
            thicknessInMillimeters = null,
            lastPriceUpdate = null
        )
        val savedCoin = coinRepository.save(coin)

        // When
        coinRepository.deleteById(savedCoin.id)
        val retrievedCoin = coinRepository.findById(savedCoin.id)

        // Then
        assertTrue(retrievedCoin.isEmpty)
    }

    @Test
    fun `should find all coins`() {
        // Given
        val coin1 = createTestCoin("Coin 1")
        val coin2 = createTestCoin("Coin 2")
        coinRepository.saveAll(listOf(coin1, coin2))

        // When
        val allCoins = coinRepository.findAll()

        // Then
        assertTrue(allCoins.size >= 2)
        assertTrue(allCoins.any { it.title == "Coin 1" })
        assertTrue(allCoins.any { it.title == "Coin 2" })
    }

    @Test
    fun `should save coin with nullable fields`() {
        // Given
        val coin = CoinEntity(
            id = UUID.randomUUID(),
            title = "Minimal Coin",
            denomination = null,
            currencyCode = "USD",
            yearOfMinting = 2023,
            issuerCountryCode = "US",
            mintMark = null,
            grade = null,
            type = CoinType.STANDARD_CIRCULATION,
            notes = null,
            numistaId = null,
            shape = CoinShape.UNKNOWN,
            weightInGrams = BigDecimal("1.00"),
            purity = null,
            metalType = null,
            rarityScore = null,
            createdAt = ZonedDateTime.now(),
            diameterInMillimeters = null,
            thicknessInMillimeters = null,
            lastPriceUpdate = null
        )

        // When
        val savedCoin = coinRepository.save(coin)
        val retrievedCoin = coinRepository.findById(savedCoin.id)

        // Then
        assertTrue(retrievedCoin.isPresent)
        assertNull(retrievedCoin.get().denomination)
        assertNull(retrievedCoin.get().mintMark)
        assertNull(retrievedCoin.get().grade)
        assertNull(retrievedCoin.get().purity)
        assertNull(retrievedCoin.get().metalType)
    }

    @Test
    fun `should handle different coin types`() {
        // Given
        val types = listOf(
            CoinType.BULLION,
            CoinType.STANDARD_CIRCULATION,
            CoinType.COMMEMORATIVE_CIRCULATION,
            CoinType.COMMEMORATIVE_NON_CIRCULATION,
            CoinType.OTHER
        )

        // When & Then
        types.forEach { type ->
            val coin = createTestCoin("Test ${type.name}").copy(type = type)
            val savedCoin = coinRepository.save(coin)
            val retrieved = coinRepository.findById(savedCoin.id)
            assertTrue(retrieved.isPresent)
            assertEquals(type, retrieved.get().type)
        }
    }

    private fun createTestCoin(title: String): CoinEntity = CoinEntity(
        id = UUID.randomUUID(),
        title = title,
        denomination = BigDecimal("1.00"),
        currencyCode = "USD",
        yearOfMinting = 2023,
        issuerCountryCode = "US",
        mintMark = null,
        grade = CoinGrade.UNCIRCULATED,
        type = CoinType.BULLION,
        notes = null,
        numistaId = null,
        shape = CoinShape.CIRCULAR,
        weightInGrams = BigDecimal("10.00"),
        purity = null,
        metalType = MetalType.GOLD,
        rarityScore = null,
        createdAt = ZonedDateTime.now(),
        diameterInMillimeters = null,
        thicknessInMillimeters = null,
        lastPriceUpdate = null
    )
}
