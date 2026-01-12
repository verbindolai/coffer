package org.coffer.coffer2.application.coinimage

import io.github.oshai.kotlinlogging.KotlinLogging
import org.coffer.coffer2.domain.coin.CoinSide
import org.coffer.coffer2.domain.coinimage.ImageUploadCommand
import org.springframework.stereotype.Service
import java.net.URI
import java.util.UUID

@Service
class CoinImageService {

    private val logger = KotlinLogging.logger {}

    companion object {
        private val ALLOWED_CONTENT_TYPES = listOf("image/jpeg", "image/png", "image/webp")
    }

    fun downloadImage(
        coinId: UUID,
        imageUrl: String,
        side: CoinSide,
    ): ImageUploadCommand? {
        logger.info { "Downloading image from: $imageUrl" }

        val url = URI(imageUrl).toURL()
        val connection = url.openConnection()
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.setRequestProperty("User-Agent", "Coffer/1.0")

        connection.getInputStream().use { inputStream ->
            val contentType = connection.contentType ?: "image/jpeg"
            val fileName = extractFileNameFromUrl(imageUrl) ?: "numista-${side.name.lowercase()}.jpg"

            if (!isAllowedContentType(contentType)) {
                logger.warn {"Skipping image with unsupported content type: $contentType"}
                return null
            }
            // Create a buffered copy since InputStream can only be read once
            val imageBytes = inputStream.readBytes()

            return ImageUploadCommand(
                coinId = coinId,
                inputStream = imageBytes.inputStream(),
                fileName = fileName,
                contentType = normalizeContentType(contentType),
                sizeBytes = imageBytes.size.toLong(),
                side = side
            )

        }
    }

    private fun extractFileNameFromUrl(url: String): String? {
        return try {
            val path = URI(url).path
            path.substringAfterLast('/')
        } catch (e: Exception) {
            null
        }
    }

    private fun isAllowedContentType(contentType: String): Boolean {
        val normalizedType = normalizeContentType(contentType)
        return normalizedType in ALLOWED_CONTENT_TYPES
    }

    private fun normalizeContentType(contentType: String): String {
        return contentType.split(';').first().trim().lowercase()
    }
}