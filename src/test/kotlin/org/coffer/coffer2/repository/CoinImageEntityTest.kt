package org.coffer.coffer2.repository

import org.coffer.coffer2.domain.coin.CoinImage
import org.coffer.coffer2.domain.coin.CoinImageSide
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.assertEquals

class CoinImageEntityTest {

    @Test
    fun `toCoinImage should convert entity to domain model correctly`() {
        // Given
        val entityId = UUID.randomUUID()
        val coinId = UUID.randomUUID()
        val createdAt = ZonedDateTime.now()
        val updatedAt = ZonedDateTime.now()

        val entity = CoinImageEntity(
            id = entityId,
            coinId = coinId,
            side = CoinImageSide.OBVERSE,
            storageKey = "coins/images/123/obverse.jpg",
            fileName = "obverse.jpg",
            contentType = "image/jpeg",
            sizeInBytes = 1024000L,
            createdAt = createdAt,
            updatedAt = updatedAt
        )

        // When
        val coinImage = entity.toCoinImage()

        // Then
        assertEquals(entityId.toString(), coinImage.id)
        assertEquals(coinId.toString(), coinImage.coinId)
        assertEquals(CoinImageSide.OBVERSE, coinImage.side)
        assertEquals("coins/images/123/obverse.jpg", coinImage.storageKey)
        assertEquals("obverse.jpg", coinImage.fileName)
        assertEquals("image/jpeg", coinImage.contentType)
        assertEquals(1024000L, coinImage.sizeInBytes)
        assertEquals(createdAt.toString(), coinImage.createdAt)
        assertEquals(updatedAt.toString(), coinImage.updatedAt)
    }

    @Test
    fun `fromCoinImage should convert domain model to entity correctly`() {
        // Given
        val id = UUID.randomUUID()
        val coinId = UUID.randomUUID()
        val createdAt = ZonedDateTime.now()
        val updatedAt = ZonedDateTime.now()

        val coinImage = CoinImage(
            id = id.toString(),
            coinId = coinId.toString(),
            side = CoinImageSide.REVERSE,
            storageKey = "coins/images/456/reverse.png",
            fileName = "reverse.png",
            contentType = "image/png",
            sizeInBytes = 2048000L,
            createdAt = createdAt.toString(),
            updatedAt = updatedAt.toString()
        )

        // When
        val entity = CoinImageEntity.fromCoinImage(coinImage)

        // Then
        assertEquals(id, entity.id)
        assertEquals(coinId, entity.coinId)
        assertEquals(CoinImageSide.REVERSE, entity.side)
        assertEquals("coins/images/456/reverse.png", entity.storageKey)
        assertEquals("reverse.png", entity.fileName)
        assertEquals("image/png", entity.contentType)
        assertEquals(2048000L, entity.sizeInBytes)
        assertEquals(createdAt, entity.createdAt)
        assertEquals(updatedAt, entity.updatedAt)
    }

    @Test
    fun `round-trip conversion should preserve all fields`() {
        // Given
        val id = UUID.randomUUID()
        val coinId = UUID.randomUUID()
        val createdAt = ZonedDateTime.now()
        val updatedAt = ZonedDateTime.now()

        val originalImage = CoinImage(
            id = id.toString(),
            coinId = coinId.toString(),
            side = CoinImageSide.OBVERSE,
            storageKey = "test/storage/key.webp",
            fileName = "test-image.webp",
            contentType = "image/webp",
            sizeInBytes = 512000L,
            createdAt = createdAt.toString(),
            updatedAt = updatedAt.toString()
        )

        // When
        val entity = CoinImageEntity.fromCoinImage(originalImage)
        val resultImage = entity.toCoinImage()

        // Then
        assertEquals(originalImage.id, resultImage.id)
        assertEquals(originalImage.coinId, resultImage.coinId)
        assertEquals(originalImage.side, resultImage.side)
        assertEquals(originalImage.storageKey, resultImage.storageKey)
        assertEquals(originalImage.fileName, resultImage.fileName)
        assertEquals(originalImage.contentType, resultImage.contentType)
        assertEquals(originalImage.sizeInBytes, resultImage.sizeInBytes)
        assertEquals(originalImage.createdAt, resultImage.createdAt)
        assertEquals(originalImage.updatedAt, resultImage.updatedAt)
    }

    @Test
    fun `should handle both OBVERSE and REVERSE sides correctly`() {
        // Given
        val coinId = UUID.randomUUID()
        val sides = listOf(CoinImageSide.OBVERSE, CoinImageSide.REVERSE)

        // When & Then
        sides.forEach { side ->
            val coinImage = CoinImage(
                id = UUID.randomUUID().toString(),
                coinId = coinId.toString(),
                side = side,
                storageKey = "storage/key",
                fileName = "file.jpg",
                contentType = "image/jpeg",
                sizeInBytes = 100000L,
                createdAt = ZonedDateTime.now().toString(),
                updatedAt = ZonedDateTime.now().toString()
            )

            val entity = CoinImageEntity.fromCoinImage(coinImage)
            val result = entity.toCoinImage()

            assertEquals(side, result.side)
        }
    }

    @Test
    fun `should handle different image content types correctly`() {
        // Given
        val coinId = UUID.randomUUID()
        val contentTypes = listOf("image/jpeg", "image/png", "image/webp", "image/gif")

        // When & Then
        contentTypes.forEach { contentType ->
            val coinImage = CoinImage(
                id = UUID.randomUUID().toString(),
                coinId = coinId.toString(),
                side = CoinImageSide.OBVERSE,
                storageKey = "storage/key",
                fileName = "file.${contentType.split("/")[1]}",
                contentType = contentType,
                sizeInBytes = 100000L,
                createdAt = ZonedDateTime.now().toString(),
                updatedAt = ZonedDateTime.now().toString()
            )

            val entity = CoinImageEntity.fromCoinImage(coinImage)
            val result = entity.toCoinImage()

            assertEquals(contentType, result.contentType)
        }
    }

    @Test
    fun `should handle various file sizes correctly`() {
        // Given
        val coinId = UUID.randomUUID()
        val fileSizes = listOf(1024L, 102400L, 1024000L, 10240000L)

        // When & Then
        fileSizes.forEach { size ->
            val coinImage = CoinImage(
                id = UUID.randomUUID().toString(),
                coinId = coinId.toString(),
                side = CoinImageSide.REVERSE,
                storageKey = "storage/key",
                fileName = "file.jpg",
                contentType = "image/jpeg",
                sizeInBytes = size,
                createdAt = ZonedDateTime.now().toString(),
                updatedAt = ZonedDateTime.now().toString()
            )

            val entity = CoinImageEntity.fromCoinImage(coinImage)
            val result = entity.toCoinImage()

            assertEquals(size, result.sizeInBytes)
        }
    }
}
