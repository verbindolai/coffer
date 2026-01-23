package org.coffer.coffer2.api

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.coffer.coffer2.application.CoinService
import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.coin.Coin
import org.coffer.coffer2.domain.coin.CoinGrade
import org.coffer.coffer2.domain.coin.CoinImage
import org.coffer.coffer2.domain.coin.CoinShape
import org.coffer.coffer2.domain.coin.CoinSide
import org.coffer.coffer2.domain.coin.CoinType
import org.coffer.coffer2.domain.coin.MintMark
import org.coffer.coffer2.domain.coin.YearOfMinting
import org.coffer.coffer2.domain.exception.CoinNotFoundException
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.HttpStatus
import java.math.BigDecimal
import java.nio.file.Files
import java.time.ZonedDateTime
import java.util.Currency
import java.util.Locale
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CoinControllerTest {

    private val coinService = mockk<CoinService>()
    private val controller = CoinController(coinService)

    // Test fixtures
    private fun createTestCoin(
        id: UUID = UUID.randomUUID(),
        title: String = "Test Coin"
    ) = Coin(
        id = id,
        title = title,
        denomination = BigDecimal("1.00"),
        currency = Currency.getInstance("USD"),
        yearOfMinting = YearOfMinting(2023),
        issuerCountry = Locale.of("", "US"),
        mintMark = MintMark("D"),
        grade = CoinGrade.UNCIRCULATED,
        type = CoinType.BULLION,
        notes = "Test notes",
        numistaId = "12345",
        shape = CoinShape.CIRCULAR,
        weightInGrams = BigDecimal("31.10"),
        purity = BigDecimal("999"),
        metalType = MetalType.GOLD,
        rarity = null,
        createdAt = ZonedDateTime.now(),
        diameterInMillimeters = BigDecimal("32.70"),
        thicknessInMillimeters = BigDecimal("2.87")
    )

    private fun createTestImage(
        id: UUID = UUID.randomUUID(),
        coinId: UUID = UUID.randomUUID(),
        side: CoinSide = CoinSide.OBVERSE
    ) = CoinImage(
        id = id,
        coinId = coinId,
        side = side,
        storageKey = "storage/key/image.jpg",
        fileName = "image.jpg",
        contentType = "image/jpeg",
        sizeInBytes = 1024,
        createdAt = ZonedDateTime.now(),
        updatedAt = ZonedDateTime.now()
    )

    // ==================== CREATE COIN TESTS ====================

    @Test
    fun `createCoin should return 201 Created with coin response`() {
        // Given
        val request = CreateCoinRequest(
            title = "American Gold Eagle",
            denomination = BigDecimal("50.00"),
            year = 2023,
            countryCode = "US",
            currency = "USD",
            mintMark = "W",
            grade = CoinGrade.UNCIRCULATED,
            coinType = CoinType.BULLION,
            notes = "Test coin",
            numistaId = "12345",
            metalType = MetalType.GOLD,
            weightInGrams = BigDecimal("31.10"),
            purity = 999,
            quantity = 1,
            rarityScore = null,
            diameterInMillimeters = BigDecimal("32.70"),
            thicknessInMillimeters = BigDecimal("2.87")
        )

        val createdCoin = createTestCoin(title = "American Gold Eagle")
        every { coinService.createCoin(any()) } returns createdCoin

        // When
        val response = controller.createCoin(request)

        // Then
        assertEquals("American Gold Eagle", response.title)
        verify(exactly = 1) { coinService.createCoin(any()) }
    }

    // ==================== GET COIN BY ID TESTS ====================

    @Test
    fun `getCoinById should return 200 OK with coin when found`() {
        // Given
        val coinId = UUID.randomUUID()
        val coin = createTestCoin(id = coinId, title = "Silver Dollar")
        every { coinService.getCoinById(coinId) } returns coin

        // When
        val response = controller.getCoinById(coinId)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(coinId.toString(), response.body!!.id)
        assertEquals("Silver Dollar", response.body!!.title)
        verify(exactly = 1) { coinService.getCoinById(coinId) }
    }

    @Test
    fun `getCoinById should return 404 Not Found when coin not found`() {
        // Given
        val coinId = UUID.randomUUID()
        every { coinService.getCoinById(coinId) } returns null

        // When
        val response = controller.getCoinById(coinId)

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
        verify(exactly = 1) { coinService.getCoinById(coinId) }
    }

    // ==================== UPDATE COIN TESTS ====================

    @Test
    fun `updateCoin should return 200 OK with updated coin`() {
        // Given
        val coinId = UUID.randomUUID()
        val request = UpdateCoinRequest(
            title = "Updated Gold Eagle",
            denomination = BigDecimal("50.00"),
            year = 2024,
            countryCode = "US",
            currency = "USD",
            mintMark = "P",
            grade = CoinGrade.PROOF,
            coinType = CoinType.BULLION,
            notes = "Updated notes",
            numistaId = "12345",
            metalType = MetalType.GOLD,
            weightInGrams = BigDecimal("31.10"),
            purity = 999,
            rarityScore = null,
            shape = CoinShape.CIRCULAR,
            diameterInMillimeters = BigDecimal("32.70"),
            thicknessInMillimeters = BigDecimal("2.87")
        )

        val updatedCoin = createTestCoin(id = coinId, title = "Updated Gold Eagle")
            .copy(grade = CoinGrade.PROOF)
        every { coinService.updateCoin(any()) } returns updatedCoin

        // When
        val response = controller.updateCoin(coinId, request)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(coinId.toString(), response.body!!.id)
        assertEquals("Updated Gold Eagle", response.body!!.title)
        assertEquals(CoinGrade.PROOF, response.body!!.grade)
        verify(exactly = 1) { coinService.updateCoin(any()) }
    }

    // ==================== DELETE COIN TESTS ====================

    @Test
    fun `deleteCoin should return 204 No Content when successful`() {
        // Given
        val coinId = UUID.randomUUID()
        every { coinService.deleteCoin(coinId) } returns Unit

        // When
        val response = controller.deleteCoin(coinId)

        // Then
        assertEquals(HttpStatus.NO_CONTENT, response.statusCode)
        verify(exactly = 1) { coinService.deleteCoin(coinId) }
    }

    // ==================== SEARCH COINS TESTS ====================

    @Test
    fun `searchCoins should return 200 OK with paginated results`() {
        // Given
        val coins = listOf(
            createTestCoin(title = "Gold Eagle"),
            createTestCoin(title = "Silver Dollar")
        )
        val pageable = PageRequest.of(0, 20, Sort.Direction.DESC, "createdAt")
        val page = PageImpl(coins, pageable, 2)
        every { coinService.searchCoins(any(), any()) } returns page

        // When
        val response = controller.searchCoins(
            country = null,
            denomination = null,
            grade = null,
            coinType = null,
            yearFrom = null,
            yearTo = null,
            metalType = null,
            shape = null,
            currency = null,
            title = null,
            numistaId = null,
            pageable = pageable
        )

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(2, response.body!!.totalElements)
        assertEquals(2, response.body!!.content.size)
        verify(exactly = 1) { coinService.searchCoins(any(), any()) }
    }

    @Test
    fun `searchCoins should pass filter parameters to service`() {
        // Given
        val coins = listOf(createTestCoin(title = "US Gold Coin"))
        val pageable = PageRequest.of(0, 20, Sort.Direction.DESC, "createdAt")
        val page = PageImpl(coins, pageable, 1)
        every { coinService.searchCoins(match {
            it.country == "US" &&
            it.grade == CoinGrade.PROOF &&
            it.metalType == MetalType.GOLD &&
            it.yearFrom == 2020 &&
            it.yearTo == 2024
        }, any()) } returns page

        // When
        val response = controller.searchCoins(
            country = "US",
            denomination = null,
            grade = CoinGrade.PROOF,
            coinType = null,
            yearFrom = 2020,
            yearTo = 2024,
            metalType = MetalType.GOLD,
            shape = null,
            currency = null,
            title = null,
            numistaId = null,
            pageable = pageable
        )

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        verify(exactly = 1) { coinService.searchCoins(match {
            it.country == "US" &&
            it.grade == CoinGrade.PROOF &&
            it.metalType == MetalType.GOLD
        }, any()) }
    }

    @Test
    fun `searchCoins should return empty page when no results`() {
        // Given
        val pageable = PageRequest.of(0, 20, Sort.Direction.DESC, "createdAt")
        val emptyPage = PageImpl<Coin>(emptyList(), pageable, 0)
        every { coinService.searchCoins(any(), any()) } returns emptyPage

        // When
        val response = controller.searchCoins(
            country = "XX",
            denomination = null,
            grade = null,
            coinType = null,
            yearFrom = null,
            yearTo = null,
            metalType = null,
            shape = null,
            currency = null,
            title = null,
            numistaId = null,
            pageable = pageable
        )

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(0, response.body!!.totalElements)
        assertTrue(response.body!!.content.isEmpty())
    }

    // ==================== GET COIN IMAGES TESTS ====================

    @Test
    fun `getCoinImages should return 200 OK with images list`() {
        // Given
        val coinId = UUID.randomUUID()
        val images = listOf(
            createTestImage(coinId = coinId, side = CoinSide.OBVERSE),
            createTestImage(coinId = coinId, side = CoinSide.REVERSE)
        )
        every { coinService.getImagesForCoin(coinId) } returns images

        // When
        val response = controller.getCoinImages(coinId)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(2, response.body!!.size)
        assertEquals(CoinSide.OBVERSE, response.body!![0].side)
        assertEquals(CoinSide.REVERSE, response.body!![1].side)
        verify(exactly = 1) { coinService.getImagesForCoin(coinId) }
    }

    @Test
    fun `getCoinImages should return empty list when no images`() {
        // Given
        val coinId = UUID.randomUUID()
        every { coinService.getImagesForCoin(coinId) } returns emptyList()

        // When
        val response = controller.getCoinImages(coinId)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertTrue(response.body!!.isEmpty())
    }

    // ==================== GET SINGLE IMAGE TESTS ====================

    @Test
    fun `getCoinImage should return 200 OK with image metadata`() {
        // Given
        val coinId = UUID.randomUUID()
        val imageId = UUID.randomUUID()
        val image = createTestImage(id = imageId, coinId = coinId, side = CoinSide.OBVERSE)
        every { coinService.getCoinImage(coinId, imageId) } returns image

        // When
        val response = controller.getCoinImage(coinId, imageId)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        assertEquals(imageId.toString(), response.body!!.id)
        assertEquals(coinId.toString(), response.body!!.coinId)
        assertEquals(CoinSide.OBVERSE, response.body!!.side)
        assertEquals("image.jpg", response.body!!.fileName)
        verify(exactly = 1) { coinService.getCoinImage(coinId, imageId) }
    }

    @Test
    fun `getCoinImage should return 404 Not Found when image not found`() {
        // Given
        val coinId = UUID.randomUUID()
        val imageId = UUID.randomUUID()
        every { coinService.getCoinImage(coinId, imageId) } returns null

        // When
        val response = controller.getCoinImage(coinId, imageId)

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }

    // ==================== GET IMAGE CONTENT TESTS ====================

    @Test
    fun `getCoinImageContent should return 200 OK with image resource`() {
        // Given
        val coinId = UUID.randomUUID()
        val imageId = UUID.randomUUID()
        val image = createTestImage(id = imageId, coinId = coinId)

        // Create a temporary file to simulate stored image
        val tempFile = Files.createTempFile("test-image", ".jpg")
        Files.write(tempFile, byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()))

        every { coinService.getImageContent(coinId, imageId) } returns (image to tempFile)

        // When
        val response = controller.getCoinImageContent(coinId, imageId)

        // Then
        assertEquals(HttpStatus.OK, response.statusCode)
        assertNotNull(response.body)
        verify(exactly = 1) { coinService.getImageContent(coinId, imageId) }

        // Cleanup
        Files.deleteIfExists(tempFile)
    }

    @Test
    fun `getCoinImageContent should return 404 Not Found when image not found`() {
        // Given
        val coinId = UUID.randomUUID()
        val imageId = UUID.randomUUID()
        every { coinService.getImageContent(coinId, imageId) } returns null

        // When
        val response = controller.getCoinImageContent(coinId, imageId)

        // Then
        assertEquals(HttpStatus.NOT_FOUND, response.statusCode)
    }
}
