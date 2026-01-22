package org.coffer.coffer2.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.coffer.coffer2.api.CoinSearchQuery
import org.coffer.coffer2.application.coinimage.CoinImageRepositoryAdapter
import org.coffer.coffer2.application.shared.ImageStorageService
import org.coffer.coffer2.domain.coin.Coin
import org.coffer.coffer2.domain.coin.CoinCreatedEvent
import org.coffer.coffer2.domain.coin.CoinDeletedEvent
import org.coffer.coffer2.domain.coin.CoinUpdatedEvent
import org.coffer.coffer2.domain.coin.CoinImage
import org.coffer.coffer2.domain.coin.CoinSide
import org.coffer.coffer2.domain.coin.CreateCoinCommand
import org.coffer.coffer2.domain.coin.UpdateCoinCommand
import org.coffer.coffer2.domain.coinimage.ImageUploadCommand
import org.coffer.coffer2.domain.exception.CoinNotFoundException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.util.UUID

@Service
class CoinService(
    private val coinRepository: CoinRepositoryAdapter,
    private val coinImageRepository: CoinImageRepositoryAdapter,
    private val applicationEventPublisher: ApplicationEventPublisher,
    private val imageStorageService: ImageStorageService
) {

    private val logger = KotlinLogging.logger {}

    @Transactional
    fun createCoin(command: CreateCoinCommand): Coin {
        val coin = command.toCoin()
        val savedCoin = coinRepository.save(coin)
        logger.info { "Coin created with id ${savedCoin.id}" }
        applicationEventPublisher.publishEvent(CoinCreatedEvent(savedCoin.id, savedCoin.numistaId))
        return savedCoin
    }

    @Transactional(readOnly = true)
    fun getCoinById(id: UUID): Coin? {
        return coinRepository.findById(id)
    }

    @Transactional
    fun updateCoin(command: UpdateCoinCommand): Coin {
        val existingCoin = coinRepository.findById(command.id)
            ?: throw CoinNotFoundException(command.id)

        val updatedCoin = command.toCoin(existingCoin)
        val savedCoin = coinRepository.save(updatedCoin)
        logger.info { "Coin updated with id ${savedCoin.id}" }
        applicationEventPublisher.publishEvent(CoinUpdatedEvent(savedCoin.id))
        return savedCoin
    }

    @Transactional
    fun deleteCoin(id: UUID) {
        if (!coinRepository.existsById(id)) {
            throw CoinNotFoundException(id)
        }
        coinRepository.deleteById(id)
        logger.info { "Coin deleted with id $id" }
        applicationEventPublisher.publishEvent(CoinDeletedEvent(id))
    }

    @Transactional(readOnly = true)
    fun searchCoins(query: CoinSearchQuery, pageable: Pageable): Page<Coin> {
        return coinRepository.search(query, pageable)
    }

    @Transactional(readOnly = true)
    fun getCoinImageSides(coinId: UUID): List<CoinSide> {
        val images = coinImageRepository.findByCoinId(coinId)
        return images.map { it.side }.distinct()
    }

    @Transactional(readOnly = true)
    fun getImagesForCoin(coinId: UUID): List<CoinImage> {
        if (!coinRepository.existsById(coinId)) {
            throw CoinNotFoundException(coinId)
        }
        return coinImageRepository.findByCoinId(coinId)
    }

    @Transactional(readOnly = true)
    fun getCoinImage(coinId: UUID, imageId: UUID): CoinImage? {
        if (!coinRepository.existsById(coinId)) {
            throw CoinNotFoundException(coinId)
        }
        return coinImageRepository.findByCoinIdAndImageId(coinId, imageId)
    }

    @Transactional(readOnly = true)
    fun getImageContent(coinId: UUID, imageId: UUID): Pair<CoinImage, Path>? {
        val image = getCoinImage(coinId, imageId) ?: return null
        val path: Path
        try {
            path = imageStorageService.retrieve(image.storageKey)
        } catch (e: NoSuchFileException) {
            logger.warn(e) { "Image not found in storage: ${image.storageKey} for coin $coinId" }
            return null
        }
        return image to path
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