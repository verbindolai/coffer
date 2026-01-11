package org.coffer.coffer2.domain.coinimage

import org.coffer.coffer2.domain.coin.CoinSide
import java.io.InputStream
import java.util.UUID

data class ImageUploadCommand(
    val coinId: UUID,
    val side: CoinSide,
    val fileName: String,
    val contentType: String,
    val sizeBytes: Long,
    val inputStream: InputStream
)