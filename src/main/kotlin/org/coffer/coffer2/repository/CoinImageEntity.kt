package org.coffer.coffer2.repository

import jakarta.persistence.*
import org.coffer.coffer2.domain.coin.CoinImage
import org.coffer.coffer2.domain.coin.CoinSide
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(name = "coin_images")
data class CoinImageEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val coinId: UUID,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val side: CoinSide,

    @Column(nullable = false)
    val storageKey: String,

    @Column(nullable = false)
    val fileName: String,

    @Column(nullable = false, length = 100)
    val contentType: String,

    @Column(nullable = false)
    val sizeInBytes: Long,

    @Column(nullable = false)
    val createdAt: ZonedDateTime = ZonedDateTime.now(),

    @Column(nullable = false)
    val updatedAt: ZonedDateTime = ZonedDateTime.now()
) {
    fun toCoinImage(): CoinImage = CoinImage(
        id = id.toString(),
        coinId = coinId.toString(),
        side = side,
        storageKey = storageKey,
        fileName = fileName,
        contentType = contentType,
        sizeInBytes = sizeInBytes,
        createdAt = createdAt.toString(),
        updatedAt = updatedAt.toString()
    )

    companion object {
        fun fromCoinImage(coinImage: CoinImage): CoinImageEntity = CoinImageEntity(
            id = UUID.fromString(coinImage.id),
            coinId = UUID.fromString(coinImage.coinId),
            side = coinImage.side,
            storageKey = coinImage.storageKey,
            fileName = coinImage.fileName,
            contentType = coinImage.contentType,
            sizeInBytes = coinImage.sizeInBytes,
            createdAt = ZonedDateTime.parse(coinImage.createdAt),
            updatedAt = ZonedDateTime.parse(coinImage.updatedAt)
        )
    }
}
