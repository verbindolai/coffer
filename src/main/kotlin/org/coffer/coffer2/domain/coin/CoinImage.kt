package org.coffer.coffer2.domain.coin

data class CoinImage(
    val id: String,
    val coinId: String,
    val side: CoinImageSide,
    val storageKey: String,
    val fileName: String,
    val contentType: String,
    val sizeInBytes: Long,
    val createdAt: String,
    val updatedAt: String,
)

enum class CoinImageSide {
    OBVERSE,
    REVERSE,
}
