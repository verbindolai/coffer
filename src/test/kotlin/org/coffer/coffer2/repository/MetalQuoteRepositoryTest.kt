package org.coffer.coffer2.repository

import org.coffer.coffer2.IntegrationTestBase
import org.coffer.coffer2.domain.MetalQuoteSource
import org.coffer.coffer2.domain.MetalType
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import jakarta.persistence.EntityManager
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Transactional
class MetalQuoteRepositoryTest : IntegrationTestBase() {

    @Autowired
    private lateinit var metalQuoteRepository: MetalQuoteRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @BeforeEach
    fun setUp() {
        // Clean up any existing metal quotes using native SQL to avoid loading invalid entities
        entityManager.createNativeQuery("DELETE FROM metal_quotes").executeUpdate()
        entityManager.flush()
    }

    @Test
    fun `should save and retrieve metal quote entity`() {
        // Given
        val entity = MetalQuoteEntity(
            id = UUID.randomUUID(),
            metalType = MetalType.GOLD,
            pricePerGram = BigDecimal("1800.50"),
            currencyCode = "EUR",
            quotedAt = ZonedDateTime.now(),
            source = MetalQuoteSource.SWISSQUOTE,
            createdAt = ZonedDateTime.now()
        )

        // When
        val saved = metalQuoteRepository.save(entity)
        val retrieved = metalQuoteRepository.findById(saved.id)

        // Then
        assertTrue(retrieved.isPresent)
        assertEquals(MetalType.GOLD, retrieved.get().metalType)
        assertEquals(0, BigDecimal("1800.50").compareTo(retrieved.get().pricePerGram))
        assertEquals("EUR", retrieved.get().currencyCode)
        assertEquals(MetalQuoteSource.SWISSQUOTE, retrieved.get().source)
    }

    @Test
    fun `should save multiple metal quotes for different metals`() {
        // Given
        val goldEntity = MetalQuoteEntity(
            id = UUID.randomUUID(),
            metalType = MetalType.GOLD,
            pricePerGram = BigDecimal("1800.50"),
            currencyCode = "EUR",
            quotedAt = ZonedDateTime.now(),
            source = MetalQuoteSource.SWISSQUOTE,
            createdAt = ZonedDateTime.now()
        )

        val silverEntity = MetalQuoteEntity(
            id = UUID.randomUUID(),
            metalType = MetalType.SILVER,
            pricePerGram = BigDecimal("25.75"),
            currencyCode = "EUR",
            quotedAt = ZonedDateTime.now(),
            source = MetalQuoteSource.SWISSQUOTE,
            createdAt = ZonedDateTime.now()
        )

        // When
        metalQuoteRepository.save(goldEntity)
        metalQuoteRepository.save(silverEntity)
        val allQuotes = metalQuoteRepository.findAll()

        // Then - should have at least these two quotes
        val goldQuotes = allQuotes.filter { it.metalType == MetalType.GOLD }
        val silverQuotes = allQuotes.filter { it.metalType == MetalType.SILVER }

        assertTrue(goldQuotes.isNotEmpty())
        assertTrue(silverQuotes.isNotEmpty())
    }

    @Test
    fun `should save quote with high precision price`() {
        // Given
        val entity = MetalQuoteEntity(
            id = UUID.randomUUID(),
            metalType = MetalType.PLATINUM,
            pricePerGram = BigDecimal("1050.1234"), // 4 decimal places
            currencyCode = "EUR",
            quotedAt = ZonedDateTime.now(),
            source = MetalQuoteSource.SWISSQUOTE,
            createdAt = ZonedDateTime.now()
        )

        // When
        val saved = metalQuoteRepository.save(entity)
        val retrieved = metalQuoteRepository.findById(saved.id)

        // Then
        assertTrue(retrieved.isPresent)
        assertEquals(0, BigDecimal("1050.1234").compareTo(retrieved.get().pricePerGram))
    }

    @Test
    fun `should save quote with UTC timezone`() {
        // Given
        val quotedAt = ZonedDateTime.now()
        val entity = MetalQuoteEntity(
            id = UUID.randomUUID(),
            metalType = MetalType.GOLD,
            pricePerGram = BigDecimal("1800.00"),
            currencyCode = "EUR",
            quotedAt = quotedAt,
            source = MetalQuoteSource.SWISSQUOTE,
            createdAt = ZonedDateTime.now()
        )

        // When
        val saved = metalQuoteRepository.save(entity)
        val retrieved = metalQuoteRepository.findById(saved.id)

        // Then
        assertTrue(retrieved.isPresent)
        assertEquals(quotedAt.toInstant(), retrieved.get().quotedAt.toInstant())
    }

    @Test
    fun `should convert entity to domain model correctly`() {
        // Given
        val entity = MetalQuoteEntity(
            id = UUID.randomUUID(),
            metalType = MetalType.GOLD,
            pricePerGram = BigDecimal("1800.50"),
            currencyCode = "EUR",
            quotedAt = ZonedDateTime.now(),
            source = MetalQuoteSource.SWISSQUOTE,
            createdAt = ZonedDateTime.now()
        )

        val saved = metalQuoteRepository.save(entity)

        // When
        val domainModel = saved.toMetalQuote()

        // Then
        assertEquals(saved.id.toString(), domainModel.id)
        assertEquals(saved.metalType, domainModel.metalType)
        assertEquals(0, saved.pricePerGram.compareTo(domainModel.pricePerGram))
        assertEquals(saved.currencyCode, domainModel.currency.currencyCode)
        assertEquals(saved.quotedAt, domainModel.quotedAt)
    }

    @Test
    fun `should handle all metal types`() {
        // Given
        val allMetalTypes = listOf(MetalType.GOLD, MetalType.SILVER, MetalType.PLATINUM, MetalType.NICKEL)
        val entities = allMetalTypes.map { metalType ->
            MetalQuoteEntity(
                id = UUID.randomUUID(),
                metalType = metalType,
                pricePerGram = BigDecimal("100.00"),
                currencyCode = "EUR",
                quotedAt = ZonedDateTime.now(),
                source = MetalQuoteSource.SWISSQUOTE,
                createdAt = ZonedDateTime.now()
            )
        }

        // When
        val savedEntities = entities.map { metalQuoteRepository.save(it) }

        // Then
        savedEntities.forEach { saved ->
            val retrieved = metalQuoteRepository.findById(saved.id)
            assertTrue(retrieved.isPresent)
            assertEquals(saved.metalType, retrieved.get().metalType)
        }
    }

    @Test
    fun `should persist createdAt timestamp`() {
        // Given
        val createdAt = ZonedDateTime.now()
        val entity = MetalQuoteEntity(
            id = UUID.randomUUID(),
            metalType = MetalType.GOLD,
            pricePerGram = BigDecimal("1800.00"),
            currencyCode = "EUR",
            quotedAt = ZonedDateTime.now(),
            source = MetalQuoteSource.SWISSQUOTE,
            createdAt = createdAt
        )

        // When
        val saved = metalQuoteRepository.save(entity)
        val retrieved = metalQuoteRepository.findById(saved.id)

        // Then
        assertTrue(retrieved.isPresent)
        assertEquals(createdAt.toInstant(), retrieved.get().createdAt.toInstant())
    }
}
