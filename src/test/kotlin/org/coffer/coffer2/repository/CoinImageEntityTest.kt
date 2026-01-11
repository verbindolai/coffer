package org.coffer.coffer2.repository

import org.coffer.coffer2.domain.coin.CoinImageSide
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.assertEquals

class CoinImageEntityTest {

    @Test
    fun `should convert entity to domain and back without losing data`() {
        // Given - entity with all fields populated
        val now = ZonedDateTime.now()
        val originalEntity = CoinImageEntity(
            id = UUID.randomUUID(),
            coinId = UUID.randomUUID(),
            side = CoinImageSide.OBVERSE,
            storageKey = "images/coin123.jpg",
            fileName = "coin123.jpg",
            contentType = "image/jpeg",
            sizeInBytes = 102400L,
            createdAt = now,
            updatedAt = now
        )

        // When - convert to domain and back
        val coinImage = originalEntity.toCoinImage()
        val roundTripEntity = CoinImageEntity.fromCoinImage(coinImage)

        // Then - verify key fields match
        assertEquals(originalEntity.id, UUID.fromString(roundTripEntity.id.toString()))
        assertEquals(originalEntity.coinId, UUID.fromString(roundTripEntity.coinId.toString()))
        assertEquals(originalEntity.side, roundTripEntity.side)
        assertEquals(originalEntity.storageKey, roundTripEntity.storageKey)
        assertEquals(originalEntity.fileName, roundTripEntity.fileName)
        assertEquals(originalEntity.sizeInBytes, roundTripEntity.sizeInBytes)
    }
}
