package org.coffer.coffer2.api

import io.swagger.v3.oas.annotations.media.Schema
import org.coffer.coffer2.domain.coin.CoinImage
import org.coffer.coffer2.domain.coin.CoinSide
import java.time.ZonedDateTime

@Schema(description = "Coin image metadata")
data class CoinImageResponse(
    @Schema(description = "Unique image identifier (UUID)", example = "660e8400-e29b-41d4-a716-446655440001")
    val id: String,

    @Schema(description = "Parent coin identifier (UUID)", example = "550e8400-e29b-41d4-a716-446655440000")
    val coinId: String,

    @Schema(description = "Side of the coin shown in the image", example = "OBVERSE")
    val side: CoinSide,

    @Schema(description = "Original file name", example = "gold-eagle-obverse.jpg")
    val fileName: String,

    @Schema(description = "MIME content type", example = "image/jpeg")
    val contentType: String,

    @Schema(description = "File size in bytes", example = "245678")
    val sizeInBytes: Long,

    @Schema(description = "Timestamp when the image was uploaded", example = "2023-06-15T10:30:00Z")
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
