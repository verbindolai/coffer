package org.coffer.coffer2.repository

import org.coffer.coffer2.IntegrationTestBase
import org.coffer.coffer2.domain.coin.CoinGrade
import org.coffer.coffer2.domain.coin.CoinShape
import org.coffer.coffer2.domain.coin.CoinType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.assertTrue

class CoinRepositoryTest : IntegrationTestBase() {

    @Autowired
    private lateinit var coinRepository: CoinRepository

    @Test
    fun `should save and retrieve coin entity - database smoke test`() {
        // Given
        val coin = CoinEntity(
            id = UUID.randomUUID(),
            title = "Test Coin",
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

        // When
        val savedCoin = coinRepository.save(coin)
        val retrievedCoin = coinRepository.findById(savedCoin.id)

        // Then
        assertTrue(retrievedCoin.isPresent)
    }
}
