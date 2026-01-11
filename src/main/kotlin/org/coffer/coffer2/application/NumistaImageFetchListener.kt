package org.coffer.coffer2.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.coffer.coffer2.domain.coin.CoinCreatedEvent
import org.coffer.coffer2.domain.coin.CoinSide
import org.coffer.coffer2.remote.numista.NumistaClient
import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import kotlin.text.isNullOrBlank

@Component
class NumistaImageFetchListener(
    private val coinService: CoinService,
    private val coinImageService: CoinImageService,
    private val numistaClient: NumistaClient
) {

    private val logger = KotlinLogging.logger {}
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handleCoinCreatedEvent(event: CoinCreatedEvent) {
        val coinId = event.coinId
        try {
            if(event.numistaId.isNullOrBlank()) {
                logger.info {"No Numista ID for coin $coinId, skipping image fetch"}
                return
            }

            logger.info {"Processing image fetch for coin $coinId with Numista ID ${event.numistaId}"}

            val sides = coinService.getCoinImageSides(event.coinId)

            val hasObverse = sides.contains(CoinSide.OBVERSE)
            val hasReverse = sides.contains(CoinSide.REVERSE)

            if (hasObverse && hasReverse) {
                logger.info {"Coin ${event.coinId} already has both obverse and reverse images, skipping fetch"}
                return
            }

            val typeInfo = numistaClient.getCoinType(event.numistaId)

            if (!hasObverse) {

                if (typeInfo.obverse?.picture.isNullOrBlank()) {
                    logger.warn {"Coin ${event.coinId} has no obverse image on Numista, skipping fetch"}
                    return
                }
                val command = coinImageService.downloadImage(
                    coinId = event.coinId,
                    imageUrl = typeInfo.obverse.picture,
                    side = CoinSide.OBVERSE,
                )

                if(command == null) {
                    logger.error {"Failed to download obverse image for coin $coinId"}
                    return
                }

                coinService.addImage(command)
                logger.info("Successfully added obverse image for coin ${event.coinId}")
            }

            if (!hasReverse) {
                if (typeInfo.reverse?.picture.isNullOrBlank()) {
                    logger.warn {"Coin ${event.coinId} has no reverse image on Numista, skipping fetch"}
                    return
                }
                val command = coinImageService.downloadImage(
                    coinId = event.coinId,
                    imageUrl = typeInfo.reverse.picture,
                    side = CoinSide.REVERSE,
                )
                if(command == null) {
                    logger.error {"Failed to download reverse image for coin $coinId"}
                    return
                }
                coinService.addImage(command)
                logger.info("Successfully added reverse image for coin ${event.coinId}")
            }

        } catch (e: Exception) {
            logger.error(e){"Failed to fetch images for coin ${event.coinId} from Numista" }
        }
    }
}
