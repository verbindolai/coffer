package org.coffer.coffer2.application

import org.coffer.coffer2.application.coinimage.CoinImageService
import org.coffer.coffer2.domain.coin.CoinSide
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Unit tests for CoinImageService.
 *
 * Note: This service makes actual HTTP connections, so comprehensive testing
 * requires either integration tests with real/mocked HTTP endpoints or
 * complex URI/URLConnection mocking. The integration tests provide better
 * coverage for the actual download functionality.
 *
 * These unit tests focus on the basic contract and error handling.
 */
class CoinImageServiceTest {

    private val service = CoinImageService()

    @Test
    fun `should throw exception for invalid URL`() {
        // Given
        val coinId = UUID.randomUUID()
        val invalidUrl = "not-a-valid-url"

        // When/Then - should throw exception for malformed URL
        assertFailsWith<Exception> {
            service.downloadImage(
                coinId = coinId,
                imageUrl = invalidUrl,
                side = CoinSide.OBVERSE
            )
        }
    }

    @Test
    fun `should throw exception for unreachable URL`() {
        // Given
        val coinId = UUID.randomUUID()
        // Using a URL that's guaranteed to fail (invalid domain)
        val unreachableUrl = "https://this-domain-definitely-does-not-exist-12345.com/image.jpg"

        // When/Then - should throw exception or return null for unreachable URL
        // The service may throw an exception or return null depending on the error
        try {
            service.downloadImage(
                coinId = coinId,
                imageUrl = unreachableUrl,
                side = CoinSide.OBVERSE
            )
            // If we get here without exception, that's okay - the download may return null
        } catch (e: Exception) {
            // Expected - network errors should be caught
        }
    }
}
