package org.coffer.coffer2.schedule

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.coffer.coffer2.application.MetalQuotesService
import org.coffer.coffer2.config.MetalQuotesProperties
import org.coffer.coffer2.domain.MetalType
import kotlin.test.Test

class MetalQuotesSchedulerTest {

    @Test
    fun `should call service with correct metal types from config`() {
        // Given
        val properties = MetalQuotesProperties(
            updateIntervalCron = "0 */5 * * * *",
            trackedMetalCodes = listOf("XAU", "XAG")
        )
        val service = mockk<MetalQuotesService>(relaxed = true)
        val scheduler = MetalQuotesFetcherScheduler(properties, service)

        // When
        scheduler.updateMetalPricesAndValuations()

        // Then
        verify(exactly = 1) {
            service.updateMetalQuotes(listOf(MetalType.GOLD, MetalType.SILVER))
        }
    }

    @Test
    fun `should call service with single metal type`() {
        // Given
        val properties = MetalQuotesProperties(
            updateIntervalCron = "0 */5 * * * *",
            trackedMetalCodes = listOf("XAU")
        )
        val service = mockk<MetalQuotesService>(relaxed = true)
        val scheduler = MetalQuotesFetcherScheduler(properties, service)

        // When
        scheduler.updateMetalPricesAndValuations()

        // Then
        verify(exactly = 1) {
            service.updateMetalQuotes(listOf(MetalType.GOLD))
        }
    }

    @Test
    fun `should call service with all metal types`() {
        // Given
        val properties = MetalQuotesProperties(
            updateIntervalCron = "0 */5 * * * *",
            trackedMetalCodes = listOf("XAU", "XAG", "XPT", "XNIK")
        )
        val service = mockk<MetalQuotesService>(relaxed = true)
        val scheduler = MetalQuotesFetcherScheduler(properties, service)

        // When
        scheduler.updateMetalPricesAndValuations()

        // Then
        verify(exactly = 1) {
            service.updateMetalQuotes(
                listOf(MetalType.GOLD, MetalType.SILVER, MetalType.PLATINUM, MetalType.NICKEL)
            )
        }
    }

    @Test
    fun `should handle empty tracked metals list`() {
        // Given
        val properties = MetalQuotesProperties(
            updateIntervalCron = "0 */5 * * * *",
            trackedMetalCodes = emptyList()
        )
        val service = mockk<MetalQuotesService>(relaxed = true)
        val scheduler = MetalQuotesFetcherScheduler(properties, service)

        // When
        scheduler.updateMetalPricesAndValuations()

        // Then
        verify(exactly = 1) {
            service.updateMetalQuotes(emptyList())
        }
    }

    @Test
    fun `should convert platinum symbol correctly`() {
        // Given
        val properties = MetalQuotesProperties(
            updateIntervalCron = "0 */5 * * * *",
            trackedMetalCodes = listOf("XPT")
        )
        val service = mockk<MetalQuotesService>(relaxed = true)
        val scheduler = MetalQuotesFetcherScheduler(properties, service)

        // When
        scheduler.updateMetalPricesAndValuations()

        // Then
        verify(exactly = 1) {
            service.updateMetalQuotes(listOf(MetalType.PLATINUM))
        }
    }

    @Test
    fun `should convert nickel symbol correctly`() {
        // Given
        val properties = MetalQuotesProperties(
            updateIntervalCron = "0 */5 * * * *",
            trackedMetalCodes = listOf("XNIK")
        )
        val service = mockk<MetalQuotesService>(relaxed = true)
        val scheduler = MetalQuotesFetcherScheduler(properties, service)

        // When
        scheduler.updateMetalPricesAndValuations()

        // Then
        verify(exactly = 1) {
            service.updateMetalQuotes(listOf(MetalType.NICKEL))
        }
    }

    @Test
    fun `should propagate exception from invalid metal code`() {
        // Given
        val properties = MetalQuotesProperties(
            updateIntervalCron = "0 */5 * * * *",
            trackedMetalCodes = listOf("INVALID")
        )
        val service = mockk<MetalQuotesService>(relaxed = true)
        val scheduler = MetalQuotesFetcherScheduler(properties, service)

        // When & Then
        try {
            scheduler.updateMetalPricesAndValuations()
            throw AssertionError("Expected IllegalArgumentException to be thrown")
        } catch (e: IllegalArgumentException) {
            // Expected - invalid metal codes should throw exception
            assert(e.message?.contains("Unknown metal symbol") == true)
        }

        // Service should not be called if metal code conversion fails
        verify(exactly = 0) {
            service.updateMetalQuotes(any())
        }
    }

    @Test
    fun `should invoke service method directly without testing scheduling`() {
        // Given
        val properties = MetalQuotesProperties(
            updateIntervalCron = "0 */5 * * * *",
            trackedMetalCodes = listOf("XAU")
        )
        val service = mockk<MetalQuotesService>(relaxed = true)
        val scheduler = MetalQuotesFetcherScheduler(properties, service)

        // When - call the method directly (not testing @Scheduled annotation)
        scheduler.updateMetalPricesAndValuations()

        // Then - verify business logic, not scheduling mechanism
        verify(exactly = 1) {
            service.updateMetalQuotes(any())
        }
    }
}
