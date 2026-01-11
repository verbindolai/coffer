package org.coffer.coffer2.integration

import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.coffer.coffer2.IntegrationTestBase
import org.coffer.coffer2.application.MetalQuotesService
import org.coffer.coffer2.config.MetalQuotesProperties
import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.remote.swissquote.SpreadProfilePrice
import org.coffer.coffer2.remote.swissquote.SwissquoteClient
import org.coffer.coffer2.remote.swissquote.SwissquoteQuoteResponse
import org.coffer.coffer2.repository.MetalQuoteRepository
import org.coffer.coffer2.schedule.MetalQuotesFetcherScheduler
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import jakarta.persistence.EntityManager
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Transactional
class MetalQuoteFlowIntegrationTest : IntegrationTestBase() {

    @Autowired
    private lateinit var metalQuotesService: MetalQuotesService

    @Autowired
    private lateinit var metalQuoteRepository: MetalQuoteRepository

    @Autowired
    private lateinit var scheduler: MetalQuotesFetcherScheduler

    @Autowired
    private lateinit var properties: MetalQuotesProperties

    @Autowired
    private lateinit var entityManager: EntityManager

    @MockkBean
    private lateinit var swissquoteClient: SwissquoteClient

    @BeforeEach
    fun setUp() {
        // Clean up any existing metal quotes using native SQL to avoid loading invalid entities
        entityManager.createNativeQuery("DELETE FROM metal_quotes").executeUpdate()
        entityManager.flush()
    }

    @Test
    fun `should fetch and persist gold quote end-to-end`() {
        // Given - mock external API response
        val apiResponse = listOf(
            SwissquoteQuoteResponse(
                spreadProfilePrices = listOf(
                    SpreadProfilePrice(
                        spreadProfile = "standard",
                        bid = BigDecimal("1800.50"),
                        ask = BigDecimal("1802.50"),
                        bidSpread = null,
                        askSpread = null
                    )
                ),
                ts = 1609459200000L
            )
        )
        every { swissquoteClient.getMetalPrice("XAU") } returns apiResponse

        // When - trigger service
        metalQuotesService.updateMetalQuotes(listOf(MetalType.GOLD))

        // Then - verify database persistence
        val allQuotes = metalQuoteRepository.findAll()
        val goldQuotes = allQuotes.filter { it.metalType == MetalType.GOLD }

        assertTrue(goldQuotes.isNotEmpty(), "Expected at least one gold quote to be saved")
        val latestGold = goldQuotes.maxByOrNull { it.createdAt }!!
        assertEquals(0, BigDecimal("1800.50").compareTo(latestGold.pricePerGram))
        assertEquals("EUR", latestGold.currencyCode)
        assertEquals("SWISSQUOTE", latestGold.source)

        verify(exactly = 1) { swissquoteClient.getMetalPrice("XAU") }
    }

    @Test
    fun `should fetch and persist multiple metal quotes end-to-end`() {
        // Given - mock external API responses
        val goldResponse = listOf(
            SwissquoteQuoteResponse(
                spreadProfilePrices = listOf(
                    SpreadProfilePrice(
                        spreadProfile = "standard",
                        bid = BigDecimal("1800.50"),
                        ask = BigDecimal("1802.50"),
                        bidSpread = null,
                        askSpread = null
                    )
                ),
                ts = 1609459200000L
            )
        )

        val silverResponse = listOf(
            SwissquoteQuoteResponse(
                spreadProfilePrices = listOf(
                    SpreadProfilePrice(
                        spreadProfile = "standard",
                        bid = BigDecimal("25.75"),
                        ask = BigDecimal("25.85"),
                        bidSpread = null,
                        askSpread = null
                    )
                ),
                ts = 1609459200000L
            )
        )

        every { swissquoteClient.getMetalPrice("XAU") } returns goldResponse
        every { swissquoteClient.getMetalPrice("XAG") } returns silverResponse

        // When - trigger service
        metalQuotesService.updateMetalQuotes(listOf(MetalType.GOLD, MetalType.SILVER))

        // Then - verify both quotes were saved
        val allQuotes = metalQuoteRepository.findAll()
        val goldQuotes = allQuotes.filter { it.metalType == MetalType.GOLD }
        val silverQuotes = allQuotes.filter { it.metalType == MetalType.SILVER }

        assertTrue(goldQuotes.isNotEmpty())
        assertTrue(silverQuotes.isNotEmpty())

        verify(exactly = 1) { swissquoteClient.getMetalPrice("XAU") }
        verify(exactly = 1) { swissquoteClient.getMetalPrice("XAG") }
    }

    @Test
    fun `should handle API returning empty response without breaking`() {
        // Given - mock empty API response
        every { swissquoteClient.getMetalPrice("XAU") } returns emptyList()

        // When - trigger service
        val initialCount = metalQuoteRepository.count()
        metalQuotesService.updateMetalQuotes(listOf(MetalType.GOLD))
        val finalCount = metalQuoteRepository.count()

        // Then - no new quote should be saved
        assertEquals(initialCount, finalCount)
        verify(exactly = 1) { swissquoteClient.getMetalPrice("XAU") }
    }

    @Test
    fun `should continue processing other metals when one API call fails`() {
        // Given - one API call fails, other succeeds
        every { swissquoteClient.getMetalPrice("XAU") } throws RuntimeException("API connection failed")

        val silverResponse = listOf(
            SwissquoteQuoteResponse(
                spreadProfilePrices = listOf(
                    SpreadProfilePrice(
                        spreadProfile = "standard",
                        bid = BigDecimal("25.75"),
                        ask = BigDecimal("25.85"),
                        bidSpread = null,
                        askSpread = null
                    )
                ),
                ts = 1609459200000L
            )
        )
        every { swissquoteClient.getMetalPrice("XAG") } returns silverResponse

        // When - trigger service for both metals
        metalQuotesService.updateMetalQuotes(listOf(MetalType.GOLD, MetalType.SILVER))

        // Then - silver should be saved despite gold failure
        val allQuotes = metalQuoteRepository.findAll()
        val silverQuotes = allQuotes.filter { it.metalType == MetalType.SILVER }

        assertTrue(silverQuotes.isNotEmpty())

        verify(exactly = 1) { swissquoteClient.getMetalPrice("XAU") }
        verify(exactly = 1) { swissquoteClient.getMetalPrice("XAG") }
    }

    @Test
    fun `scheduler should trigger service and persist quotes`() {
        // Given - mock external API responses for configured metals
        val goldResponse = listOf(
            SwissquoteQuoteResponse(
                spreadProfilePrices = listOf(
                    SpreadProfilePrice(
                        spreadProfile = "standard",
                        bid = BigDecimal("1850.00"),
                        ask = BigDecimal("1852.00"),
                        bidSpread = null,
                        askSpread = null
                    )
                ),
                ts = 1609459200000L
            )
        )

        val silverResponse = listOf(
            SwissquoteQuoteResponse(
                spreadProfilePrices = listOf(
                    SpreadProfilePrice(
                        spreadProfile = "standard",
                        bid = BigDecimal("26.00"),
                        ask = BigDecimal("26.10"),
                        bidSpread = null,
                        askSpread = null
                    )
                ),
                ts = 1609459200000L
            )
        )

        every { swissquoteClient.getMetalPrice("XAU") } returns goldResponse
        every { swissquoteClient.getMetalPrice("XAG") } returns silverResponse

        // When - trigger scheduler (not testing cron, just the method)
        scheduler.updateMetalPricesAndValuations()

        // Then - quotes should be persisted
        val allQuotes = metalQuoteRepository.findAll()

        // Since config has XAU and XAG, both should be called
        verify(exactly = 1) { swissquoteClient.getMetalPrice("XAU") }
        verify(exactly = 1) { swissquoteClient.getMetalPrice("XAG") }

        // At least one quote should exist
        assertTrue(allQuotes.isNotEmpty())
    }

    @Test
    fun `should persist quotes with correct domain model conversion`() {
        // Given
        val apiResponse = listOf(
            SwissquoteQuoteResponse(
                spreadProfilePrices = listOf(
                    SpreadProfilePrice(
                        spreadProfile = "standard",
                        bid = BigDecimal("1050.1234"),
                        ask = BigDecimal("1052.5678"),
                        bidSpread = null,
                        askSpread = null
                    )
                ),
                ts = 1609459200000L
            )
        )
        every { swissquoteClient.getMetalPrice("XPT") } returns apiResponse

        // When
        metalQuotesService.updateMetalQuotes(listOf(MetalType.PLATINUM))

        // Then - verify entity was converted correctly
        val allQuotes = metalQuoteRepository.findAll()
        val platinumQuotes = allQuotes.filter { it.metalType == MetalType.PLATINUM }

        assertTrue(platinumQuotes.isNotEmpty())
        val quote = platinumQuotes.maxByOrNull { it.createdAt }!!

        // Verify conversion to domain model and back maintains data
        val domainQuote = quote.toMetalQuote()
        assertEquals(MetalType.PLATINUM, domainQuote.metalType)
        assertEquals(0, BigDecimal("1050.1234").compareTo(domainQuote.pricePerGram))
        assertEquals("EUR", domainQuote.currency.currencyCode)
    }

    @Test
    fun `should handle transactional behavior correctly`() {
        // Given - valid API response
        val apiResponse = listOf(
            SwissquoteQuoteResponse(
                spreadProfilePrices = listOf(
                    SpreadProfilePrice(
                        spreadProfile = "standard",
                        bid = BigDecimal("1800.00"),
                        ask = BigDecimal("1802.00"),
                        bidSpread = null,
                        askSpread = null
                    )
                ),
                ts = 1609459200000L
            )
        )
        every { swissquoteClient.getMetalPrice("XAU") } returns apiResponse

        // When - call service within transaction
        val beforeCount = metalQuoteRepository.count()
        metalQuotesService.updateMetalQuotes(listOf(MetalType.GOLD))
        val afterCount = metalQuoteRepository.count()

        // Then - transaction should commit successfully
        assertTrue(afterCount > beforeCount, "Expected quote count to increase after successful save")
    }
}
