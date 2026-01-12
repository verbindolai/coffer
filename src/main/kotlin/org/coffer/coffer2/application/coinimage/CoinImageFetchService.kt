package org.coffer.coffer2.application.coinimage

import io.github.oshai.kotlinlogging.KotlinLogging
import org.coffer.coffer2.application.CoinService
import org.coffer.coffer2.domain.coin.CoinSide
import org.coffer.coffer2.remote.numista.NumistaClient
import org.coffer.coffer2.remote.numista.NumistaTypeResponse
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class CoinImageFetchService(
    private val coinService: CoinService,
    private val coinImageService: CoinImageService,
    private val numistaClient: NumistaClient
) {

    private val logger = KotlinLogging.logger {}

    /**
     * Fetches missing coin images from Numista for a given coin.
     *
     * @param coinId The ID of the coin to fetch images for
     * @param numistaId The Numista type ID to fetch images from
     * @return true if at least one image was successfully fetched, false otherwise
     */
    fun fetchMissingImagesFromNumista(coinId: UUID, numistaId: String): Boolean {
        logger.info { "Processing image fetch for coin $coinId with Numista ID $numistaId" }

        val existingSides = coinService.getCoinImageSides(coinId)

        if (existingSides.containsAll(CoinSide.entries)) {
            logger.info { "Coin $coinId already has all images, skipping fetch" }
            return false
        }

        val typeInfo = numistaClient.getCoinType(numistaId)
        val sidesToFetch = determineSidesToFetch(existingSides, typeInfo)

        if (sidesToFetch.isEmpty()) {
            logger.info { "No new images available for coin $coinId" }
            return false
        }

        var successCount = 0
        for ((side, imageUrl) in sidesToFetch) {
            if (fetchAndSaveImage(coinId, side, imageUrl)) {
                successCount++
            }
        }

        return successCount > 0
    }

    private fun determineSidesToFetch(
        existingSides: List<CoinSide>,
        typeInfo: NumistaTypeResponse
    ): List<Pair<CoinSide, String>> {
        val sidesToFetch = mutableListOf<Pair<CoinSide, String>>()

        if (!existingSides.contains(CoinSide.OBVERSE) && !typeInfo.obverse?.picture.isNullOrBlank()) {
            sidesToFetch.add(CoinSide.OBVERSE to typeInfo.obverse.picture)
        }

        if (!existingSides.contains(CoinSide.REVERSE) && !typeInfo.reverse?.picture.isNullOrBlank()) {
            sidesToFetch.add(CoinSide.REVERSE to typeInfo.reverse.picture)
        }

        return sidesToFetch
    }

    private fun fetchAndSaveImage(coinId: UUID, side: CoinSide, imageUrl: String): Boolean {
        return try {
            val command = coinImageService.downloadImage(
                coinId = coinId,
                imageUrl = imageUrl,
                side = side
            )

            if (command == null) {
                logger.error { "Failed to download ${side.name.lowercase()} image for coin $coinId from $imageUrl" }
                return false
            }

            coinService.addImage(command)
            logger.info { "Successfully added ${side.name.lowercase()} image for coin $coinId" }
            true
        } catch (e: Exception) {
            logger.error(e) { "Error fetching ${side.name.lowercase()} image for coin $coinId from $imageUrl" }
            false
        }
    }
}
