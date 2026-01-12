package org.coffer.coffer2.application

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.coffer.coffer2.application.coinimage.CoinImageFetchService
import org.coffer.coffer2.application.coinimage.CoinImageService
import org.coffer.coffer2.domain.coin.CoinSide
import org.coffer.coffer2.domain.coinimage.ImageUploadCommand
import org.coffer.coffer2.remote.numista.NumistaClient
import org.coffer.coffer2.remote.numista.NumistaImageInfo
import org.coffer.coffer2.remote.numista.NumistaTypeResponse
import java.io.ByteArrayInputStream
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CoinImageFetchServiceTest {

    private val coinService = mockk<CoinService>()
    private val coinImageService = mockk<CoinImageService>()
    private val numistaClient = mockk<NumistaClient>()
    private val service = CoinImageFetchService(coinService, coinImageService, numistaClient)

    @Test
    fun `should return false when numistaId is blank`() {
        // Given
        val coinId = UUID.randomUUID()

        // When
        val result = service.fetchMissingImagesFromNumista(coinId, "")

        // Then
        assertFalse(result)
        verify(exactly = 0) { coinService.getCoinImageSides(any()) }
        verify(exactly = 0) { numistaClient.getCoinType(any()) }
    }

    @Test
    fun `should return false when coin already has all images`() {
        // Given
        val coinId = UUID.randomUUID()
        val numistaId = "12345"
        every { coinService.getCoinImageSides(coinId) } returns listOf(CoinSide.OBVERSE, CoinSide.REVERSE)

        // When
        val result = service.fetchMissingImagesFromNumista(coinId, numistaId)

        // Then
        assertFalse(result)
        verify(exactly = 1) { coinService.getCoinImageSides(coinId) }
        verify(exactly = 0) { numistaClient.getCoinType(any()) }
    }

    @Test
    fun `should successfully fetch obverse image when missing`() {
        // Given
        val coinId = UUID.randomUUID()
        val numistaId = "12345"
        val imageUrl = "https://example.com/obverse.jpg"

        every { coinService.getCoinImageSides(coinId) } returns listOf(CoinSide.REVERSE)
        every { numistaClient.getCoinType(numistaId) } returns NumistaTypeResponse(
            id = numistaId,
            title = "Test Coin",
            obverse = NumistaImageInfo(picture = imageUrl, thumbnail = null, description = null),
            reverse = null,
            issuer = null,
            min_year = null,
            max_year = null,
            weight = null,
            size = null,
            thickness = null,
            composition = null,
            ruler = null,
            mints = null,
            value = null,
            type = null,
            shape = null
        )

        val mockCommand = ImageUploadCommand(
            coinId = coinId,
            side = CoinSide.OBVERSE,
            fileName = "obverse.jpg",
            contentType = "image/jpeg",
            sizeBytes = 1024,
            inputStream = ByteArrayInputStream(ByteArray(0))
        )

        every { coinImageService.downloadImage(coinId, imageUrl, CoinSide.OBVERSE) } returns mockCommand
        every { coinService.addImage(mockCommand) } returns mockk()

        // When
        val result = service.fetchMissingImagesFromNumista(coinId, numistaId)

        // Then
        assertTrue(result)
        verify(exactly = 1) { coinService.getCoinImageSides(coinId) }
        verify(exactly = 1) { numistaClient.getCoinType(numistaId) }
        verify(exactly = 1) { coinImageService.downloadImage(coinId, imageUrl, CoinSide.OBVERSE) }
        verify(exactly = 1) { coinService.addImage(mockCommand) }
    }

    @Test
    fun `should successfully fetch reverse image when missing`() {
        // Given
        val coinId = UUID.randomUUID()
        val numistaId = "12345"
        val imageUrl = "https://example.com/reverse.jpg"

        every { coinService.getCoinImageSides(coinId) } returns listOf(CoinSide.OBVERSE)
        every { numistaClient.getCoinType(numistaId) } returns NumistaTypeResponse(
            id = numistaId,
            title = "Test Coin",
            obverse = null,
            reverse = NumistaImageInfo(picture = imageUrl, thumbnail = null, description = null),
            issuer = null,
            min_year = null,
            max_year = null,
            weight = null,
            size = null,
            thickness = null,
            composition = null,
            ruler = null,
            mints = null,
            value = null,
            type = null,
            shape = null
        )

        val mockCommand = ImageUploadCommand(
            coinId = coinId,
            side = CoinSide.REVERSE,
            fileName = "reverse.jpg",
            contentType = "image/jpeg",
            sizeBytes = 1024,
            inputStream = ByteArrayInputStream(ByteArray(0))
        )

        every { coinImageService.downloadImage(coinId, imageUrl, CoinSide.REVERSE) } returns mockCommand
        every { coinService.addImage(mockCommand) } returns mockk()

        // When
        val result = service.fetchMissingImagesFromNumista(coinId, numistaId)

        // Then
        assertTrue(result)
        verify(exactly = 1) { coinService.getCoinImageSides(coinId) }
        verify(exactly = 1) { numistaClient.getCoinType(numistaId) }
        verify(exactly = 1) { coinImageService.downloadImage(coinId, imageUrl, CoinSide.REVERSE) }
        verify(exactly = 1) { coinService.addImage(mockCommand) }
    }

    @Test
    fun `should fetch both obverse and reverse images when both missing`() {
        // Given
        val coinId = UUID.randomUUID()
        val numistaId = "12345"
        val obverseUrl = "https://example.com/obverse.jpg"
        val reverseUrl = "https://example.com/reverse.jpg"

        every { coinService.getCoinImageSides(coinId) } returns emptyList()
        every { numistaClient.getCoinType(numistaId) } returns NumistaTypeResponse(
            id = numistaId,
            title = "Test Coin",
            obverse = NumistaImageInfo(picture = obverseUrl, thumbnail = null, description = null),
            reverse = NumistaImageInfo(picture = reverseUrl, thumbnail = null, description = null),
            issuer = null,
            min_year = null,
            max_year = null,
            weight = null,
            size = null,
            thickness = null,
            composition = null,
            ruler = null,
            mints = null,
            value = null,
            type = null,
            shape = null
        )

        val obverseCommand = ImageUploadCommand(
            coinId = coinId,
            side = CoinSide.OBVERSE,
            fileName = "obverse.jpg",
            contentType = "image/jpeg",
            sizeBytes = 1024,
            inputStream = ByteArrayInputStream(ByteArray(0))
        )

        val reverseCommand = ImageUploadCommand(
            coinId = coinId,
            side = CoinSide.REVERSE,
            fileName = "reverse.jpg",
            contentType = "image/jpeg",
            sizeBytes = 1024,
            inputStream = ByteArrayInputStream(ByteArray(0))
        )

        every { coinImageService.downloadImage(coinId, obverseUrl, CoinSide.OBVERSE) } returns obverseCommand
        every { coinImageService.downloadImage(coinId, reverseUrl, CoinSide.REVERSE) } returns reverseCommand
        every { coinService.addImage(obverseCommand) } returns mockk()
        every { coinService.addImage(reverseCommand) } returns mockk()

        // When
        val result = service.fetchMissingImagesFromNumista(coinId, numistaId)

        // Then
        assertTrue(result)
        verify(exactly = 1) { coinService.getCoinImageSides(coinId) }
        verify(exactly = 1) { numistaClient.getCoinType(numistaId) }
        verify(exactly = 1) { coinImageService.downloadImage(coinId, obverseUrl, CoinSide.OBVERSE) }
        verify(exactly = 1) { coinImageService.downloadImage(coinId, reverseUrl, CoinSide.REVERSE) }
        verify(exactly = 1) { coinService.addImage(obverseCommand) }
        verify(exactly = 1) { coinService.addImage(reverseCommand) }
    }

    @Test
    fun `should return false when no images available from Numista`() {
        // Given
        val coinId = UUID.randomUUID()
        val numistaId = "12345"

        every { coinService.getCoinImageSides(coinId) } returns emptyList()
        every { numistaClient.getCoinType(numistaId) } returns NumistaTypeResponse(
            id = numistaId,
            title = "Test Coin",
            obverse = null,
            reverse = null,
            issuer = null,
            min_year = null,
            max_year = null,
            weight = null,
            size = null,
            thickness = null,
            composition = null,
            ruler = null,
            mints = null,
            value = null,
            type = null,
            shape = null
        )

        // When
        val result = service.fetchMissingImagesFromNumista(coinId, numistaId)

        // Then
        assertFalse(result)
        verify(exactly = 1) { coinService.getCoinImageSides(coinId) }
        verify(exactly = 1) { numistaClient.getCoinType(numistaId) }
        verify(exactly = 0) { coinImageService.downloadImage(any(), any(), any()) }
    }

    @Test
    fun `should return false when Numista has images but they are blank strings`() {
        // Given
        val coinId = UUID.randomUUID()
        val numistaId = "12345"

        every { coinService.getCoinImageSides(coinId) } returns emptyList()
        every { numistaClient.getCoinType(numistaId) } returns NumistaTypeResponse(
            id = numistaId,
            title = "Test Coin",
            obverse = NumistaImageInfo(picture = "", thumbnail = null, description = null),
            reverse = NumistaImageInfo(picture = " ", thumbnail = null, description = null),
            issuer = null,
            min_year = null,
            max_year = null,
            weight = null,
            size = null,
            thickness = null,
            composition = null,
            ruler = null,
            mints = null,
            value = null,
            type = null,
            shape = null
        )

        // When
        val result = service.fetchMissingImagesFromNumista(coinId, numistaId)

        // Then
        assertFalse(result)
        verify(exactly = 1) { coinService.getCoinImageSides(coinId) }
        verify(exactly = 1) { numistaClient.getCoinType(numistaId) }
        verify(exactly = 0) { coinImageService.downloadImage(any(), any(), any()) }
    }

    @Test
    fun `should return false when download fails and returns null`() {
        // Given
        val coinId = UUID.randomUUID()
        val numistaId = "12345"
        val imageUrl = "https://example.com/obverse.jpg"

        every { coinService.getCoinImageSides(coinId) } returns emptyList()
        every { numistaClient.getCoinType(numistaId) } returns NumistaTypeResponse(
            id = numistaId,
            title = "Test Coin",
            obverse = NumistaImageInfo(picture = imageUrl, thumbnail = null, description = null),
            reverse = null,
            issuer = null,
            min_year = null,
            max_year = null,
            weight = null,
            size = null,
            thickness = null,
            composition = null,
            ruler = null,
            mints = null,
            value = null,
            type = null,
            shape = null
        )

        every { coinImageService.downloadImage(coinId, imageUrl, CoinSide.OBVERSE) } returns null

        // When
        val result = service.fetchMissingImagesFromNumista(coinId, numistaId)

        // Then
        assertFalse(result)
        verify(exactly = 1) { coinImageService.downloadImage(coinId, imageUrl, CoinSide.OBVERSE) }
        verify(exactly = 0) { coinService.addImage(any()) }
    }

    @Test
    fun `should handle exception during download gracefully`() {
        // Given
        val coinId = UUID.randomUUID()
        val numistaId = "12345"
        val imageUrl = "https://example.com/obverse.jpg"

        every { coinService.getCoinImageSides(coinId) } returns emptyList()
        every { numistaClient.getCoinType(numistaId) } returns NumistaTypeResponse(
            id = numistaId,
            title = "Test Coin",
            obverse = NumistaImageInfo(picture = imageUrl, thumbnail = null, description = null),
            reverse = null,
            issuer = null,
            min_year = null,
            max_year = null,
            weight = null,
            size = null,
            thickness = null,
            composition = null,
            ruler = null,
            mints = null,
            value = null,
            type = null,
            shape = null
        )

        every { coinImageService.downloadImage(coinId, imageUrl, CoinSide.OBVERSE) } throws RuntimeException("Network error")

        // When - should not throw exception
        val result = service.fetchMissingImagesFromNumista(coinId, numistaId)

        // Then
        assertFalse(result)
        verify(exactly = 1) { coinImageService.downloadImage(coinId, imageUrl, CoinSide.OBVERSE) }
        verify(exactly = 0) { coinService.addImage(any()) }
    }

    @Test
    fun `should continue to next image when one download fails`() {
        // Given
        val coinId = UUID.randomUUID()
        val numistaId = "12345"
        val obverseUrl = "https://example.com/obverse.jpg"
        val reverseUrl = "https://example.com/reverse.jpg"

        every { coinService.getCoinImageSides(coinId) } returns emptyList()
        every { numistaClient.getCoinType(numistaId) } returns NumistaTypeResponse(
            id = numistaId,
            title = "Test Coin",
            obverse = NumistaImageInfo(picture = obverseUrl, thumbnail = null, description = null),
            reverse = NumistaImageInfo(picture = reverseUrl, thumbnail = null, description = null),
            issuer = null,
            min_year = null,
            max_year = null,
            weight = null,
            size = null,
            thickness = null,
            composition = null,
            ruler = null,
            mints = null,
            value = null,
            type = null,
            shape = null
        )

        val reverseCommand = ImageUploadCommand(
            coinId = coinId,
            side = CoinSide.REVERSE,
            fileName = "reverse.jpg",
            contentType = "image/jpeg",
            sizeBytes = 1024,
            inputStream = ByteArrayInputStream(ByteArray(0))
        )

        every { coinImageService.downloadImage(coinId, obverseUrl, CoinSide.OBVERSE) } throws RuntimeException("Network error")
        every { coinImageService.downloadImage(coinId, reverseUrl, CoinSide.REVERSE) } returns reverseCommand
        every { coinService.addImage(reverseCommand) } returns mockk()

        // When
        val result = service.fetchMissingImagesFromNumista(coinId, numistaId)

        // Then - should still return true because one image was successfully fetched
        assertTrue(result)
        verify(exactly = 1) { coinImageService.downloadImage(coinId, obverseUrl, CoinSide.OBVERSE) }
        verify(exactly = 1) { coinImageService.downloadImage(coinId, reverseUrl, CoinSide.REVERSE) }
        verify(exactly = 1) { coinService.addImage(reverseCommand) }
    }

    @Test
    fun `should not fetch images that already exist`() {
        // Given
        val coinId = UUID.randomUUID()
        val numistaId = "12345"
        val obverseUrl = "https://example.com/obverse.jpg"
        val reverseUrl = "https://example.com/reverse.jpg"

        every { coinService.getCoinImageSides(coinId) } returns listOf(CoinSide.OBVERSE)
        every { numistaClient.getCoinType(numistaId) } returns NumistaTypeResponse(
            id = numistaId,
            title = "Test Coin",
            obverse = NumistaImageInfo(picture = obverseUrl, thumbnail = null, description = null),
            reverse = NumistaImageInfo(picture = reverseUrl, thumbnail = null, description = null),
            issuer = null,
            min_year = null,
            max_year = null,
            weight = null,
            size = null,
            thickness = null,
            composition = null,
            ruler = null,
            mints = null,
            value = null,
            type = null,
            shape = null
        )

        val reverseCommand = ImageUploadCommand(
            coinId = coinId,
            side = CoinSide.REVERSE,
            fileName = "reverse.jpg",
            contentType = "image/jpeg",
            sizeBytes = 1024,
            inputStream = ByteArrayInputStream(ByteArray(0))
        )

        every { coinImageService.downloadImage(coinId, reverseUrl, CoinSide.REVERSE) } returns reverseCommand
        every { coinService.addImage(reverseCommand) } returns mockk()

        // When
        val result = service.fetchMissingImagesFromNumista(coinId, numistaId)

        // Then
        assertTrue(result)
        verify(exactly = 1) { coinService.getCoinImageSides(coinId) }
        verify(exactly = 1) { numistaClient.getCoinType(numistaId) }
        verify(exactly = 0) { coinImageService.downloadImage(coinId, obverseUrl, CoinSide.OBVERSE) }
        verify(exactly = 1) { coinImageService.downloadImage(coinId, reverseUrl, CoinSide.REVERSE) }
        verify(exactly = 1) { coinService.addImage(reverseCommand) }
    }
}
