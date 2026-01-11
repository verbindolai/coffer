package org.coffer.coffer2.domain.coin

import java.time.Instant
import java.time.ZonedDateTime
import java.util.UUID

data class CoinImage(
    val id: UUID,
    val coinId: UUID,
    val side: CoinSide,
    val storageKey: String,
    val fileName: String,
    val contentType: String,
    val sizeInBytes: Long,
    val createdAt: ZonedDateTime,
    val updatedAt: ZonedDateTime,
) {
    companion object {
        fun create(
            coinId: UUID,
            side: CoinSide,
            storageKey: String,
            fileName: String,
            contentType: String,
            sizeInBytes: Long
        ): CoinImage {
            return CoinImage(
                id = UUID.randomUUID(),
                coinId = coinId,
                side = side,
                storageKey = storageKey,
                fileName = fileName,
                contentType = contentType,
                sizeInBytes = sizeInBytes,
                createdAt = ZonedDateTime.now(),
                updatedAt = ZonedDateTime.now()
            )
        }
    }
}
