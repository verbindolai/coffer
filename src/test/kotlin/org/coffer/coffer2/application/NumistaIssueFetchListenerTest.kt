package org.coffer.coffer2.application

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.coffer.coffer2.application.issue.IssueFetchService
import org.coffer.coffer2.domain.coin.CoinCreatedEvent
import java.util.UUID
import kotlin.test.Test

class NumistaIssueFetchListenerTest {

    private val issueFetchService = mockk<IssueFetchService>()
    private val listener = NumistaIssueFetchListener(issueFetchService)

    @Test
    fun `should call fetch service when coin created with valid numistaId`() {
        // Given
        val coinId = UUID.randomUUID()
        val numistaId = "12345"
        val event = CoinCreatedEvent(coinId, numistaId)

        every { issueFetchService.fetchIssuesFromNumista(coinId, numistaId) } returns true

        // When
        listener.handleCoinCreatedEvent(event)

        // Then
        verify(exactly = 1) { issueFetchService.fetchIssuesFromNumista(coinId, any()) }
    }

    @Test
    fun `should not call fetch service when numistaId is null`() {
        // Given
        val coinId = UUID.randomUUID()
        val event = CoinCreatedEvent(coinId, null)

        // When
        listener.handleCoinCreatedEvent(event)

        // Then
        verify(exactly = 0) { issueFetchService.fetchIssuesFromNumista(any(), any()) }
    }

    @Test
    fun `should not call fetch service when numistaId is blank`() {
        // Given
        val coinId = UUID.randomUUID()
        val event = CoinCreatedEvent(coinId, "   ")

        // When
        listener.handleCoinCreatedEvent(event)

        // Then
        verify(exactly = 0) { issueFetchService.fetchIssuesFromNumista(any(), any()) }
    }

    @Test
    fun `should not call fetch service when numistaId is empty`() {
        // Given
        val coinId = UUID.randomUUID()
        val event = CoinCreatedEvent(coinId, "")

        // When
        listener.handleCoinCreatedEvent(event)

        // Then
        verify(exactly = 0) { issueFetchService.fetchIssuesFromNumista(any(), any()) }
    }

    @Test
    fun `should handle exception from fetch service gracefully`() {
        // Given
        val coinId = UUID.randomUUID()
        val numistaId = "12345"
        val event = CoinCreatedEvent(coinId, numistaId)

        every { issueFetchService.fetchIssuesFromNumista(coinId, any()) } throws RuntimeException("API error")

        // When - should not throw exception
        listener.handleCoinCreatedEvent(event)

        // Then
        verify(exactly = 1) { issueFetchService.fetchIssuesFromNumista(coinId, any()) }
    }

    @Test
    fun `should handle multiple events independently`() {
        // Given
        val coinId1 = UUID.randomUUID()
        val coinId2 = UUID.randomUUID()
        val numistaId1 = "12345"
        val numistaId2 = "67890"
        val event1 = CoinCreatedEvent(coinId1, numistaId1)
        val event2 = CoinCreatedEvent(coinId2, numistaId2)

        every { issueFetchService.fetchIssuesFromNumista(coinId1, any()) } returns true
        every { issueFetchService.fetchIssuesFromNumista(coinId2, any()) } returns true

        // When
        listener.handleCoinCreatedEvent(event1)
        listener.handleCoinCreatedEvent(event2)

        // Then
        verify(exactly = 1) { issueFetchService.fetchIssuesFromNumista(coinId1, any()) }
        verify(exactly = 1) { issueFetchService.fetchIssuesFromNumista(coinId2, any()) }
    }

    @Test
    fun `should continue processing after one event fails`() {
        // Given
        val coinId1 = UUID.randomUUID()
        val coinId2 = UUID.randomUUID()
        val numistaId1 = "12345"
        val numistaId2 = "67890"
        val event1 = CoinCreatedEvent(coinId1, numistaId1)
        val event2 = CoinCreatedEvent(coinId2, numistaId2)

        every { issueFetchService.fetchIssuesFromNumista(coinId1, any()) } throws RuntimeException("API error")
        every { issueFetchService.fetchIssuesFromNumista(coinId2, any()) } returns true

        // When - both should be called without throwing
        listener.handleCoinCreatedEvent(event1)
        listener.handleCoinCreatedEvent(event2)

        // Then
        verify(exactly = 1) { issueFetchService.fetchIssuesFromNumista(coinId1, any()) }
        verify(exactly = 1) { issueFetchService.fetchIssuesFromNumista(coinId2, any()) }
    }

    @Test
    fun `should work when fetch service returns false`() {
        // Given
        val coinId = UUID.randomUUID()
        val numistaId = "12345"
        val event = CoinCreatedEvent(coinId, numistaId)

        every { issueFetchService.fetchIssuesFromNumista(coinId, any()) } returns false

        // When
        listener.handleCoinCreatedEvent(event)

        // Then - should still call the service
        verify(exactly = 1) { issueFetchService.fetchIssuesFromNumista(coinId, any()) }
    }
}
