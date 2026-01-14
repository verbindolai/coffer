package org.coffer.coffer2.api

import org.coffer.coffer2.domain.coin.CoinImage
import org.coffer.coffer2.domain.coin.CoinSide
import java.time.ZonedDateTime

data class CoinImageResponse(
    val id: String,
    val coinId: String,
    val side: CoinSide,
    val fileName: String,
    val contentType: String,
    val sizeInBytes: Long,
    val createdAt: ZonedDateTime,
) {
    companion object {
        fun from(coinImage: CoinImage): CoinImageResponse =
            CoinImageResponse(
                id = coinImage.id.toString(),
                coinId = coinImage.coinId.toString(),
                side = coinImage.side,
                fileName = coinImage.fileName,
                contentType = coinImage.contentType,
                sizeInBytes = coinImage.sizeInBytes,
                createdAt = coinImage.createdAt,
            )
    }
}
