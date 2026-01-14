package org.coffer.coffer2.application

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.coffer.coffer2.api.CoinSearchQuery
import org.coffer.coffer2.application.coinimage.CoinImageRepositoryAdapter
import org.coffer.coffer2.application.shared.ImageStorageService
import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.coin.Coin
import org.coffer.coffer2.domain.coin.CoinCreatedEvent
import org.coffer.coffer2.domain.coin.CoinGrade
import org.coffer.coffer2.domain.coin.CoinImage
import org.coffer.coffer2.domain.coin.CoinShape
import org.coffer.coffer2.domain.coin.CoinSide
import org.coffer.coffer2.domain.coin.CoinType
import org.coffer.coffer2.domain.coin.CreateCoinCommand
import org.coffer.coffer2.domain.coin.MintMark
import org.coffer.coffer2.domain.coin.UpdateCoinCommand
import org.coffer.coffer2.domain.coin.YearOfMinting
import org.coffer.coffer2.domain.exception.CoinNotFoundException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import java.math.BigDecimal
import java.nio.file.Path
import java.time.ZonedDateTime
import java.util.Currency
import java.util.Locale
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class CoinServiceTest {

    private val coinRepository = mockk<CoinRepositoryAdapter>()
    private val coinImageRepository = mockk<CoinImageRepositoryAdapter>()
    private val applicationEventPublisher = mockk<ApplicationEventPublisher>(relaxed = true)
    private val imageStorageService = mockk<ImageStorageService>()

    private val service = CoinService(
        coinRepository,
        coinImageRepository,
        applicationEventPublisher,
        imageStorageService
    )

    // Test fixtures
    private fun createTestCoin(
        id: UUID = UUID.randomUUID(),
        title: String = "Test Coin",
        grade: CoinGrade = CoinGrade.UNCIRCULATED
    ) = Coin(
        id = id,
        title = title,
        denomination = BigDecimal("1.00"),
        currency = Currency.getInstance("USD"),
        yearOfMinting = YearOfMinting(2023),
        issuerCountry = Locale.of("", "US"),
        mintMark = MintMark("D"),
        grade = grade,
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

    private fun createTestCommand() = CreateCoinCommand(
        title = "Test Coin",
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
    fun `createCoin should save coin and publish event`() {
        // Given
        val command = createTestCommand()
        val coinSlot = slot<Coin>()
        every { coinRepository.save(capture(coinSlot)) } answers { coinSlot.captured }

        // When
        val result = service.createCoin(command)

        // Then
        assertNotNull(result)
        assertEquals("Test Coin", result.title)
        assertEquals(CoinGrade.UNCIRCULATED, result.grade)
        verify(exactly = 1) { coinRepository.save(any()) }
        verify(exactly = 1) { applicationEventPublisher.publishEvent(any<CoinCreatedEvent>()) }
    }

    @Test
    fun `createCoin should generate new UUID for coin`() {
        // Given
        val command = createTestCommand()
        val savedCoins = mutableListOf<Coin>()
        every { coinRepository.save(capture(savedCoins)) } answers { savedCoins.last() }

        // When
        service.createCoin(command)
        service.createCoin(command)

        // Then
        assertEquals(2, savedCoins.size)
        assert(savedCoins[0].id != savedCoins[1].id)
    }

    // ==================== GET COIN BY ID TESTS ====================

    @Test
    fun `getCoinById should return coin when found`() {
        // Given
        val coinId = UUID.randomUUID()
        val expectedCoin = createTestCoin(id = coinId)
        every { coinRepository.findById(coinId) } returns expectedCoin

        // When
        val result = service.getCoinById(coinId)

        // Then
        assertNotNull(result)
        assertEquals(coinId, result.id)
        assertEquals("Test Coin", result.title)
        verify(exactly = 1) { coinRepository.findById(coinId) }
    }

    @Test
    fun `getCoinById should return null when not found`() {
        // Given
        val coinId = UUID.randomUUID()
        every { coinRepository.findById(coinId) } returns null

        // When
        val result = service.getCoinById(coinId)

        // Then
        assertNull(result)
        verify(exactly = 1) { coinRepository.findById(coinId) }
    }

    // ==================== UPDATE COIN TESTS ====================

    @Test
    fun `updateCoin should update existing coin`() {
        // Given
        val coinId = UUID.randomUUID()
        val existingCoin = createTestCoin(id = coinId, title = "Old Title")
        val updateCommand = UpdateCoinCommand(
            id = coinId,
            title = "Updated Title",
            denomination = BigDecimal("2.00"),
            currency = Currency.getInstance("EUR"),
            yearOfMinting = YearOfMinting(2024),
            issuerCountry = Locale.of("", "DE"),
            mintMark = MintMark("M"),
            grade = CoinGrade.PROOF,
            type = CoinType.COMMEMORATIVE_CIRCULATION,
            notes = "Updated notes",
            numistaId = "54321",
            shape = CoinShape.HEPTAGON,
            weightInGrams = BigDecimal("28.00"),
            purity = BigDecimal("925"),
            metalType = MetalType.SILVER,
            rarity = null,
            diameterInMillimeters = BigDecimal("38.00"),
            thicknessInMillimeters = BigDecimal("3.00")
        )

        every { coinRepository.findById(coinId) } returns existingCoin
        val savedCoinSlot = slot<Coin>()
        every { coinRepository.save(capture(savedCoinSlot)) } answers { savedCoinSlot.captured }

        // When
        val result = service.updateCoin(updateCommand)

        // Then
        assertEquals("Updated Title", result.title)
        assertEquals(CoinGrade.PROOF, result.grade)
        assertEquals(coinId, result.id)
        verify(exactly = 1) { coinRepository.findById(coinId) }
        verify(exactly = 1) { coinRepository.save(any()) }
    }

    @Test
    fun `updateCoin should throw CoinNotFoundException when coin not found`() {
        // Given
        val coinId = UUID.randomUUID()
        val updateCommand = UpdateCoinCommand(
            id = coinId,
            title = "Updated Title",
            denomination = BigDecimal("1.00"),
            currency = Currency.getInstance("USD"),
            yearOfMinting = YearOfMinting(2023),
            issuerCountry = Locale.of("", "US"),
            mintMark = null,
            grade = CoinGrade.FINE,
            type = CoinType.STANDARD_CIRCULATION,
            notes = null,
            numistaId = null,
            shape = CoinShape.CIRCULAR,
            weightInGrams = BigDecimal("5.00"),
            purity = null,
            metalType = null,
            rarity = null,
            diameterInMillimeters = null,
            thicknessInMillimeters = null
        )

        every { coinRepository.findById(coinId) } returns null

        // When/Then
        assertFailsWith<CoinNotFoundException> {
            service.updateCoin(updateCommand)
        }

        verify(exactly = 1) { coinRepository.findById(coinId) }
        verify(exactly = 0) { coinRepository.save(any()) }
    }

    @Test
    fun `updateCoin should preserve createdAt from existing coin`() {
        // Given
        val coinId = UUID.randomUUID()
        val originalCreatedAt = ZonedDateTime.now().minusDays(10)
        val existingCoin = createTestCoin(id = coinId).copy(createdAt = originalCreatedAt)
        val updateCommand = UpdateCoinCommand(
            id = coinId,
            title = "Updated Title",
            denomination = BigDecimal("1.00"),
            currency = Currency.getInstance("USD"),
            yearOfMinting = YearOfMinting(2023),
            issuerCountry = Locale.of("", "US"),
            mintMark = null,
            grade = CoinGrade.FINE,
            type = CoinType.STANDARD_CIRCULATION,
            notes = null,
            numistaId = null,
            shape = CoinShape.CIRCULAR,
            weightInGrams = BigDecimal("5.00"),
            purity = null,
            metalType = null,
            rarity = null,
            diameterInMillimeters = null,
            thicknessInMillimeters = null
        )

        every { coinRepository.findById(coinId) } returns existingCoin
        val savedCoinSlot = slot<Coin>()
        every { coinRepository.save(capture(savedCoinSlot)) } answers { savedCoinSlot.captured }

        // When
        val result = service.updateCoin(updateCommand)

        // Then
        assertEquals(originalCreatedAt, result.createdAt)
    }

    // ==================== DELETE COIN TESTS ====================

    @Test
    fun `deleteCoin should delete existing coin`() {
        // Given
        val coinId = UUID.randomUUID()
        every { coinRepository.existsById(coinId) } returns true
        every { coinRepository.deleteById(coinId) } returns Unit

        // When
        service.deleteCoin(coinId)

        // Then
        verify(exactly = 1) { coinRepository.existsById(coinId) }
        verify(exactly = 1) { coinRepository.deleteById(coinId) }
    }

    @Test
    fun `deleteCoin should throw CoinNotFoundException when coin not found`() {
        // Given
        val coinId = UUID.randomUUID()
        every { coinRepository.existsById(coinId) } returns false

        // When/Then
        assertFailsWith<CoinNotFoundException> {
            service.deleteCoin(coinId)
        }

        verify(exactly = 1) { coinRepository.existsById(coinId) }
        verify(exactly = 0) { coinRepository.deleteById(any()) }
    }

    // ==================== SEARCH COINS TESTS ====================

    @Test
    fun `searchCoins should return paginated results`() {
        // Given
        val query = CoinSearchQuery(country = "US", grade = CoinGrade.UNCIRCULATED)
        val pageable = PageRequest.of(0, 10)
        val coins = listOf(
            createTestCoin(title = "Coin 1"),
            createTestCoin(title = "Coin 2")
        )
        val page = PageImpl(coins, pageable, 2)
        every { coinRepository.search(query, pageable) } returns page

        // When
        val result = service.searchCoins(query, pageable)

        // Then
        assertEquals(2, result.totalElements)
        assertEquals(2, result.content.size)
        verify(exactly = 1) { coinRepository.search(query, pageable) }
    }

    @Test
    fun `searchCoins should return empty page when no results`() {
        // Given
        val query = CoinSearchQuery(country = "XX")
        val pageable = PageRequest.of(0, 10)
        val emptyPage = PageImpl<Coin>(emptyList(), pageable, 0)
        every { coinRepository.search(query, pageable) } returns emptyPage

        // When
        val result = service.searchCoins(query, pageable)

        // Then
        assertEquals(0, result.totalElements)
        assert(result.content.isEmpty())
        verify(exactly = 1) { coinRepository.search(query, pageable) }
    }

    @Test
    fun `searchCoins should pass all query parameters to repository`() {
        // Given
        val query = CoinSearchQuery(
            country = "US",
            denomination = "1.00",
            grade = CoinGrade.PROOF,
            coinType = CoinType.BULLION,
            yearFrom = 2020,
            yearTo = 2024,
            metalType = MetalType.GOLD,
            shape = CoinShape.CIRCULAR,
            currency = "USD",
            title = "Eagle",
            numistaId = "12345"
        )
        val pageable = PageRequest.of(0, 20)
        val emptyPage = PageImpl<Coin>(emptyList(), pageable, 0)

        val capturedQuery = slot<CoinSearchQuery>()
        every { coinRepository.search(capture(capturedQuery), pageable) } returns emptyPage

        // When
        service.searchCoins(query, pageable)

        // Then
        assertEquals("US", capturedQuery.captured.country)
        assertEquals(CoinGrade.PROOF, capturedQuery.captured.grade)
        assertEquals(MetalType.GOLD, capturedQuery.captured.metalType)
        assertEquals(2020, capturedQuery.captured.yearFrom)
        assertEquals(2024, capturedQuery.captured.yearTo)
    }

    // ==================== GET IMAGES FOR COIN TESTS ====================

    @Test
    fun `getImagesForCoin should return images when coin exists`() {
        // Given
        val coinId = UUID.randomUUID()
        val images = listOf(
            createTestImage(coinId = coinId, side = CoinSide.OBVERSE),
            createTestImage(coinId = coinId, side = CoinSide.REVERSE)
        )
        every { coinRepository.existsById(coinId) } returns true
        every { coinImageRepository.findByCoinId(coinId) } returns images

        // When
        val result = service.getImagesForCoin(coinId)

        // Then
        assertEquals(2, result.size)
        verify(exactly = 1) { coinRepository.existsById(coinId) }
        verify(exactly = 1) { coinImageRepository.findByCoinId(coinId) }
    }

    @Test
    fun `getImagesForCoin should throw CoinNotFoundException when coin not found`() {
        // Given
        val coinId = UUID.randomUUID()
        every { coinRepository.existsById(coinId) } returns false

        // When/Then
        assertFailsWith<CoinNotFoundException> {
            service.getImagesForCoin(coinId)
        }

        verify(exactly = 1) { coinRepository.existsById(coinId) }
        verify(exactly = 0) { coinImageRepository.findByCoinId(any()) }
    }

    @Test
    fun `getImagesForCoin should return empty list when no images exist`() {
        // Given
        val coinId = UUID.randomUUID()
        every { coinRepository.existsById(coinId) } returns true
        every { coinImageRepository.findByCoinId(coinId) } returns emptyList()

        // When
        val result = service.getImagesForCoin(coinId)

        // Then
        assert(result.isEmpty())
    }

    // ==================== GET COIN IMAGE TESTS ====================

    @Test
    fun `getCoinImage should return image when found`() {
        // Given
        val coinId = UUID.randomUUID()
        val imageId = UUID.randomUUID()
        val image = createTestImage(id = imageId, coinId = coinId)
        every { coinRepository.existsById(coinId) } returns true
        every { coinImageRepository.findByCoinIdAndImageId(coinId, imageId) } returns image

        // When
        val result = service.getCoinImage(coinId, imageId)

        // Then
        assertNotNull(result)
        assertEquals(imageId, result.id)
        assertEquals(coinId, result.coinId)
    }

    @Test
    fun `getCoinImage should return null when image not found`() {
        // Given
        val coinId = UUID.randomUUID()
        val imageId = UUID.randomUUID()
        every { coinRepository.existsById(coinId) } returns true
        every { coinImageRepository.findByCoinIdAndImageId(coinId, imageId) } returns null

        // When
        val result = service.getCoinImage(coinId, imageId)

        // Then
        assertNull(result)
    }

    @Test
    fun `getCoinImage should throw CoinNotFoundException when coin not found`() {
        // Given
        val coinId = UUID.randomUUID()
        val imageId = UUID.randomUUID()
        every { coinRepository.existsById(coinId) } returns false

        // When/Then
        assertFailsWith<CoinNotFoundException> {
            service.getCoinImage(coinId, imageId)
        }
    }

    // ==================== GET IMAGE CONTENT TESTS ====================

    @Test
    fun `getImageContent should return image and path when found`() {
        // Given
        val coinId = UUID.randomUUID()
        val imageId = UUID.randomUUID()
        val image = createTestImage(id = imageId, coinId = coinId)
        val expectedPath = Path.of("/storage/images/image.jpg")

        every { coinRepository.existsById(coinId) } returns true
        every { coinImageRepository.findByCoinIdAndImageId(coinId, imageId) } returns image
        every { imageStorageService.retrieve(image.storageKey) } returns expectedPath

        // When
        val result = service.getImageContent(coinId, imageId)

        // Then
        assertNotNull(result)
        assertEquals(image, result.first)
        assertEquals(expectedPath, result.second)
        verify(exactly = 1) { imageStorageService.retrieve(image.storageKey) }
    }

    @Test
    fun `getImageContent should return null when image not found`() {
        // Given
        val coinId = UUID.randomUUID()
        val imageId = UUID.randomUUID()
        every { coinRepository.existsById(coinId) } returns true
        every { coinImageRepository.findByCoinIdAndImageId(coinId, imageId) } returns null

        // When
        val result = service.getImageContent(coinId, imageId)

        // Then
        assertNull(result)
        verify(exactly = 0) { imageStorageService.retrieve(any()) }
    }

    @Test
    fun `getImageContent should throw CoinNotFoundException when coin not found`() {
        // Given
        val coinId = UUID.randomUUID()
        val imageId = UUID.randomUUID()
        every { coinRepository.existsById(coinId) } returns false

        // When/Then
        assertFailsWith<CoinNotFoundException> {
            service.getImageContent(coinId, imageId)
        }
    }
}
