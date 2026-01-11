package org.coffer.coffer2.repository

import org.coffer.coffer2.IntegrationTestBase
import org.coffer.coffer2.domain.coin.CoinGrade
import org.coffer.coffer2.domain.coin.CoinImageSide
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

class CoinImageRepositoryTest : IntegrationTestBase() {

    @Autowired
    private lateinit var coinImageRepository: CoinImageRepository

    @Autowired
    private lateinit var coinRepository: CoinRepository

    @Test
    fun `should save and retrieve coin image entity`() {
        // Given
        val coin = createTestCoin()
        val savedCoin = coinRepository.save(coin)

        val coinImage = CoinImageEntity(
            id = UUID.randomUUID(),
            coinId = savedCoin.id,
            side = CoinImageSide.OBVERSE,
            storageKey = "coins/images/${savedCoin.id}/obverse.jpg",
            fileName = "obverse.jpg",
            contentType = "image/jpeg",
            sizeInBytes = 1024000L,
            createdAt = ZonedDateTime.now(),
            updatedAt = ZonedDateTime.now()
        )

        // When
        val savedImage = coinImageRepository.save(coinImage)
        val retrievedImage = coinImageRepository.findById(savedImage.id)

        // Then
        assertTrue(retrievedImage.isPresent)
        assertEquals(savedCoin.id, retrievedImage.get().coinId)
        assertEquals(CoinImageSide.OBVERSE, retrievedImage.get().side)
        assertEquals("obverse.jpg", retrievedImage.get().fileName)
        assertEquals("image/jpeg", retrievedImage.get().contentType)
        assertEquals(1024000L, retrievedImage.get().sizeInBytes)
    }

    @Test
    fun `should cascade delete coin images when coin is deleted`() {
        // Given
        val coin = createTestCoin()
        val savedCoin = coinRepository.save(coin)

        val coinImage = createTestCoinImage(savedCoin.id, CoinImageSide.OBVERSE)
        val savedImage = coinImageRepository.save(coinImage)

        // When
        coinRepository.deleteById(savedCoin.id)
        coinRepository.flush()
        val retrievedImage = coinImageRepository.findById(savedImage.id)

        // Then
        assertTrue(retrievedImage.isEmpty)
    }

    @Test
    fun `should fail to save coin image with non-existent coin id`() {
        // Given
        val nonExistentCoinId = UUID.randomUUID()
        val coinImage = createTestCoinImage(nonExistentCoinId, CoinImageSide.OBVERSE)

        // When & Then
        assertFailsWith<DataIntegrityViolationException> {
            coinImageRepository.save(coinImage)
            coinImageRepository.flush()
        }
    }

    @Test
    fun `should enforce unique constraint on coin_id and side`() {
        // Given
        val coin = createTestCoin()
        val savedCoin = coinRepository.save(coin)

        val image1 = createTestCoinImage(savedCoin.id, CoinImageSide.OBVERSE)
        coinImageRepository.save(image1)

        // When & Then
        val image2 = createTestCoinImage(savedCoin.id, CoinImageSide.OBVERSE)
        assertFailsWith<DataIntegrityViolationException> {
            coinImageRepository.save(image2)
            coinImageRepository.flush()
        }
    }

    @Test
    fun `should allow same coin to have both obverse and reverse images`() {
        // Given
        val coin = createTestCoin()
        val savedCoin = coinRepository.save(coin)

        val obverseImage = createTestCoinImage(savedCoin.id, CoinImageSide.OBVERSE)
            .copy(fileName = "obverse.jpg")
        val reverseImage = createTestCoinImage(savedCoin.id, CoinImageSide.REVERSE)
            .copy(fileName = "reverse.jpg")

        // When
        val savedObverse = coinImageRepository.save(obverseImage)
        val savedReverse = coinImageRepository.save(reverseImage)

        val allImages = coinImageRepository.findAll()
        val coinImages = allImages.filter { it.coinId == savedCoin.id }

        // Then
        assertEquals(2, coinImages.size)
        assertTrue(coinImages.any { it.side == CoinImageSide.OBVERSE })
        assertTrue(coinImages.any { it.side == CoinImageSide.REVERSE })
    }

    @Test
    fun `should update existing coin image`() {
        // Given
        val coin = createTestCoin()
        val savedCoin = coinRepository.save(coin)

        val coinImage = createTestCoinImage(savedCoin.id, CoinImageSide.OBVERSE)
        val savedImage = coinImageRepository.save(coinImage)

        // When
        val updatedImage = savedImage.copy(
            fileName = "updated.jpg",
            sizeInBytes = 2048000L,
            updatedAt = ZonedDateTime.now()
        )
        coinImageRepository.save(updatedImage)
        val retrievedImage = coinImageRepository.findById(savedImage.id)

        // Then
        assertTrue(retrievedImage.isPresent)
        assertEquals("updated.jpg", retrievedImage.get().fileName)
        assertEquals(2048000L, retrievedImage.get().sizeInBytes)
    }

    @Test
    fun `should delete coin image`() {
        // Given
        val coin = createTestCoin()
        val savedCoin = coinRepository.save(coin)

        val coinImage = createTestCoinImage(savedCoin.id, CoinImageSide.REVERSE)
        val savedImage = coinImageRepository.save(coinImage)

        // When
        coinImageRepository.deleteById(savedImage.id)
        val retrievedImage = coinImageRepository.findById(savedImage.id)

        // Then
        assertTrue(retrievedImage.isEmpty)
    }

    @Test
    fun `should handle different content types`() {
        // Given
        val coin = createTestCoin()
        val savedCoin = coinRepository.save(coin)

        val contentTypes = listOf("image/jpeg", "image/png", "image/webp")

        // When & Then
        contentTypes.forEachIndexed { index, contentType ->
            // Create a new coin for each test to avoid unique constraint violation
            val testCoin = createTestCoin()
            val testSavedCoin = coinRepository.save(testCoin)

            val image = createTestCoinImage(testSavedCoin.id, CoinImageSide.OBVERSE)
                .copy(contentType = contentType)
            val savedImage = coinImageRepository.save(image)
            val retrieved = coinImageRepository.findById(savedImage.id)
            assertTrue(retrieved.isPresent)
            assertEquals(contentType, retrieved.get().contentType)
        }
    }

    @Test
    fun `should handle different file sizes`() {
        // Given
        val fileSizes = listOf(1024L, 102400L, 1024000L, 10240000L)

        // When & Then
        fileSizes.forEach { size ->
            val coin = createTestCoin()
            val savedCoin = coinRepository.save(coin)

            val image = createTestCoinImage(savedCoin.id, CoinImageSide.OBVERSE)
                .copy(sizeInBytes = size)
            val savedImage = coinImageRepository.save(image)
            val retrieved = coinImageRepository.findById(savedImage.id)
            assertTrue(retrieved.isPresent)
            assertEquals(size, retrieved.get().sizeInBytes)
        }
    }

    private fun createTestCoin(): CoinEntity = CoinEntity(
        id = UUID.randomUUID(),
        title = "Test Coin for Image ${UUID.randomUUID()}",
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

    private fun createTestCoinImage(coinId: UUID, side: CoinImageSide): CoinImageEntity =
        CoinImageEntity(
            id = UUID.randomUUID(),
            coinId = coinId,
            side = side,
            storageKey = "test/storage/${coinId}/${side.name.lowercase()}.jpg",
            fileName = "${side.name.lowercase()}.jpg",
            contentType = "image/jpeg",
            sizeInBytes = 1024000L,
            createdAt = ZonedDateTime.now(),
            updatedAt = ZonedDateTime.now()
        )
}
