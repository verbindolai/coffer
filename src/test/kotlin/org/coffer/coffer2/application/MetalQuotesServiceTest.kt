package org.coffer.coffer2.application

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.coffer.coffer2.domain.MetalQuote
import org.coffer.coffer2.domain.MetalQuoteSource
import org.coffer.coffer2.domain.MetalType
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.Test

class MetalQuotesServiceTest {

    @Test
    fun `should fetch and save quote for single metal type`() {
        // Given
        val fetcher = mockk<MetalQuotesFetcherService>()
        val repository = mockk<MetalQuoteRepositoryAdapter>()
        val service = MetalQuotesService(fetcher, repository)

        val goldQuote = MetalQuote(
            metalType = MetalType.GOLD,
            pricePerGram = BigDecimal("1800.50"),
            currency = Currency.getInstance("EUR"),
            quotedAt = ZonedDateTime.now(),
            source = MetalQuoteSource.SWISSQUOTE
        )

        every { fetcher.getMetalQuote(MetalType.GOLD) } returns goldQuote
        every { repository.save(goldQuote) } returns goldQuote

        // When
        service.updateMetalQuotes(listOf(MetalType.GOLD))

        // Then
        verify(exactly = 1) { fetcher.getMetalQuote(MetalType.GOLD) }
        verify(exactly = 1) { repository.save(goldQuote) }
    }

    @Test
    fun `should fetch and save quotes for multiple metal types`() {
        // Given
        val fetcher = mockk<MetalQuotesFetcherService>()
        val repository = mockk<MetalQuoteRepositoryAdapter>(relaxed = true)
        val service = MetalQuotesService(fetcher, repository)

        val goldQuote = MetalQuote(
            metalType = MetalType.GOLD,
            pricePerGram = BigDecimal("1800.50"),
            currency = Currency.getInstance("EUR"),
            quotedAt = ZonedDateTime.now(),
            source = MetalQuoteSource.SWISSQUOTE
        )

        val silverQuote = MetalQuote(
            metalType = MetalType.SILVER,
            pricePerGram = BigDecimal("25.75"),
            currency = Currency.getInstance("EUR"),
            quotedAt = ZonedDateTime.now(),
            source = MetalQuoteSource.SWISSQUOTE
        )

        every { fetcher.getMetalQuote(MetalType.GOLD) } returns goldQuote
        every { fetcher.getMetalQuote(MetalType.SILVER) } returns silverQuote

        // When
        service.updateMetalQuotes(listOf(MetalType.GOLD, MetalType.SILVER))

        // Then
        verify(exactly = 1) { fetcher.getMetalQuote(MetalType.GOLD) }
        verify(exactly = 1) { fetcher.getMetalQuote(MetalType.SILVER) }
        verify(exactly = 1) { repository.save(goldQuote) }
        verify(exactly = 1) { repository.save(silverQuote) }
    }

    @Test
    fun `should not save when fetcher returns null`() {
        // Given
        val fetcher = mockk<MetalQuotesFetcherService>()
        val repository = mockk<MetalQuoteRepositoryAdapter>()
        val service = MetalQuotesService(fetcher, repository)

        every { fetcher.getMetalQuote(MetalType.GOLD) } returns null

        // When
        service.updateMetalQuotes(listOf(MetalType.GOLD))

        // Then
        verify(exactly = 1) { fetcher.getMetalQuote(MetalType.GOLD) }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `should continue processing other metals when one fetcher returns null`() {
        // Given
        val fetcher = mockk<MetalQuotesFetcherService>()
        val repository = mockk<MetalQuoteRepositoryAdapter>(relaxed = true)
        val service = MetalQuotesService(fetcher, repository)

        val silverQuote = MetalQuote(
            metalType = MetalType.SILVER,
            pricePerGram = BigDecimal("25.75"),
            currency = Currency.getInstance("EUR"),
            quotedAt = ZonedDateTime.now(),
            source = MetalQuoteSource.SWISSQUOTE
        )

        every { fetcher.getMetalQuote(MetalType.GOLD) } returns null
        every { fetcher.getMetalQuote(MetalType.SILVER) } returns silverQuote

        // When
        service.updateMetalQuotes(listOf(MetalType.GOLD, MetalType.SILVER))

        // Then
        verify(exactly = 1) { fetcher.getMetalQuote(MetalType.GOLD) }
        verify(exactly = 1) { fetcher.getMetalQuote(MetalType.SILVER) }
        verify(exactly = 0) { repository.save(match { it.metalType == MetalType.GOLD }) }
        verify(exactly = 1) { repository.save(silverQuote) }
    }

    @Test
    fun `should catch and log exceptions without propagating`() {
        // Given
        val fetcher = mockk<MetalQuotesFetcherService>()
        val repository = mockk<MetalQuoteRepositoryAdapter>()
        val service = MetalQuotesService(fetcher, repository)

        every { fetcher.getMetalQuote(MetalType.GOLD) } throws RuntimeException("API connection failed")

        // When - should not throw exception
        service.updateMetalQuotes(listOf(MetalType.GOLD))

        // Then
        verify(exactly = 1) { fetcher.getMetalQuote(MetalType.GOLD) }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `should continue processing other metals when one throws exception`() {
        // Given
        val fetcher = mockk<MetalQuotesFetcherService>()
        val repository = mockk<MetalQuoteRepositoryAdapter>(relaxed = true)
        val service = MetalQuotesService(fetcher, repository)

        val silverQuote = MetalQuote(
            metalType = MetalType.SILVER,
            pricePerGram = BigDecimal("25.75"),
            currency = Currency.getInstance("EUR"),
            quotedAt = ZonedDateTime.now(),
            source = MetalQuoteSource.SWISSQUOTE
        )

        every { fetcher.getMetalQuote(MetalType.GOLD) } throws RuntimeException("API connection failed")
        every { fetcher.getMetalQuote(MetalType.SILVER) } returns silverQuote

        // When - should not throw exception
        service.updateMetalQuotes(listOf(MetalType.GOLD, MetalType.SILVER))

        // Then - both fetchers should be called, but only silver should be saved
        verify(exactly = 1) { fetcher.getMetalQuote(MetalType.GOLD) }
        verify(exactly = 1) { fetcher.getMetalQuote(MetalType.SILVER) }
        verify(exactly = 1) { repository.save(silverQuote) }
    }

    @Test
    fun `should handle repository save exception without propagating`() {
        // Given
        val fetcher = mockk<MetalQuotesFetcherService>()
        val repository = mockk<MetalQuoteRepositoryAdapter>()
        val service = MetalQuotesService(fetcher, repository)

        val goldQuote = MetalQuote(
            metalType = MetalType.GOLD,
            pricePerGram = BigDecimal("1800.50"),
            currency = Currency.getInstance("EUR"),
            quotedAt = ZonedDateTime.now(),
            source = MetalQuoteSource.SWISSQUOTE
        )

        every { fetcher.getMetalQuote(MetalType.GOLD) } returns goldQuote
        every { repository.save(goldQuote) } throws RuntimeException("Database connection failed")

        // When - should not throw exception
        service.updateMetalQuotes(listOf(MetalType.GOLD))

        // Then
        verify(exactly = 1) { fetcher.getMetalQuote(MetalType.GOLD) }
        verify(exactly = 1) { repository.save(goldQuote) }
    }

    @Test
    fun `should process empty list without errors`() {
        // Given
        val fetcher = mockk<MetalQuotesFetcherService>()
        val repository = mockk<MetalQuoteRepositoryAdapter>()
        val service = MetalQuotesService(fetcher, repository)

        // When
        service.updateMetalQuotes(emptyList())

        // Then
        verify(exactly = 0) { fetcher.getMetalQuote(any()) }
        verify(exactly = 0) { repository.save(any()) }
    }

    @Test
    fun `should process all metal types when requested`() {
        // Given
        val fetcher = mockk<MetalQuotesFetcherService>()
        val repository = mockk<MetalQuoteRepositoryAdapter>(relaxed = true)
        val service = MetalQuotesService(fetcher, repository)

        val allMetalTypes = listOf(MetalType.GOLD, MetalType.SILVER, MetalType.PLATINUM, MetalType.NICKEL)

        allMetalTypes.forEach { metalType ->
            val quote = MetalQuote(
                metalType = metalType,
                pricePerGram = BigDecimal("100.00"),
                currency = Currency.getInstance("EUR"),
                quotedAt = ZonedDateTime.now(),
                source = MetalQuoteSource.SWISSQUOTE
            )
            every { fetcher.getMetalQuote(metalType) } returns quote
        }

        // When
        service.updateMetalQuotes(allMetalTypes)

        // Then
        allMetalTypes.forEach { metalType ->
            verify(exactly = 1) { fetcher.getMetalQuote(metalType) }
        }
        verify(exactly = 4) { repository.save(any()) }
    }
}
