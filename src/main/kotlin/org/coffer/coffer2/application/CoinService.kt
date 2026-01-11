package org.coffer.coffer2.application

import org.coffer.coffer2.domain.coin.Coin
import org.coffer.coffer2.domain.coin.CoinCreatedEvent
import org.coffer.coffer2.domain.coin.CoinImage
import org.coffer.coffer2.domain.coin.CoinSide
import org.coffer.coffer2.domain.coin.CreateCoinCommand
import org.coffer.coffer2.domain.coinimage.ImageUploadCommand
import org.coffer.coffer2.domain.exception.CoinNotFoundException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.io.inputStream

@Service
class CoinService(
    private val coinRepository: CoinRepositoryAdapter,
    private val coinImageRepository: CoinImageRepositoryAdapter,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val imageStorageService: ImageStorageService
) {
    fun createCoin(command: CreateCoinCommand): Coin {
        val coin = command.toCoin()
        val savedCoin = coinRepository.save(coin)
        applicationEventPublisher.publishEvent(CoinCreatedEvent(savedCoin.id, savedCoin.numistaId))
        return savedCoin
    }

    @Transactional(readOnly = true)
    fun getCoinImageSides(coinId: UUID): List<CoinSide> {
        val images = coinImageRepository.findByCoinId(coinId)
        return images.map { it.side }.distinct()
    }

    @Transactional
    fun addImage(command: ImageUploadCommand): CoinImage {
        val coin = coinRepository.findById(command.coinId) ?: throw CoinNotFoundException(command.coinId)

        val storageKey = imageStorageService.store(
            command.inputStream,
            command.fileName,
            command.contentType
        )

        val image = CoinImage.create(
            coinId = coin.id,
            side = command.side,
            storageKey = storageKey,
            fileName = command.fileName,
            contentType = command.contentType,
            sizeInBytes = command.sizeBytes
        )

        return coinImageRepository.save(image)

    }
}