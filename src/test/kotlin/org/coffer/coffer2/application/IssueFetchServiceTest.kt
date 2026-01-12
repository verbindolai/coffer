package org.coffer.coffer2.application

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.coffer.coffer2.application.issue.IssueFetchService
import org.coffer.coffer2.application.issue.IssueRepositoryAdapter
import org.coffer.coffer2.domain.coin.Issue
import org.coffer.coffer2.remote.numista.NumistaClient
import org.coffer.coffer2.remote.numista.NumistaIssueResponse
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IssueFetchServiceTest {

    private val issueRepositoryAdapter = mockk<IssueRepositoryAdapter>()
    private val numistaClient = mockk<NumistaClient>()
    private val service = IssueFetchService(issueRepositoryAdapter, numistaClient)

    @Test
    fun `should return false when coin already has linked issues`() {
        // Given
        val coinId = UUID.randomUUID()
        val coinTypeNumistaId = "12345"
        every { issueRepositoryAdapter.coinHasIssues(coinId) } returns true

        // When
        val result = service.fetchIssuesFromNumista(coinId, coinTypeNumistaId)

        // Then
        assertFalse(result)
        verify(exactly = 1) { issueRepositoryAdapter.coinHasIssues(coinId) }
        verify(exactly = 0) { issueRepositoryAdapter.findIssuesByNumistaId(any()) }
        verify(exactly = 0) { numistaClient.getIssues(any()) }
    }

    @Test
    fun `should reuse existing issues without calling API`() {
        // Given
        val coinId = UUID.randomUUID()
        val coinTypeNumistaId = "12345"
        val issue1Id = UUID.randomUUID()
        val issue2Id = UUID.randomUUID()
        val existingIssues = listOf(
            Issue(id = issue1Id, numistaId = "101", year = 2020, mintage = 1_000_000, mintLetter = "D", comment = "Regular issue", isProof = false),
            Issue(id = issue2Id, numistaId = "102", year = 2021, mintage = 500_000, mintLetter = "A", comment = "Proof", isProof = true)
        )

        every { issueRepositoryAdapter.coinHasIssues(coinId) } returns false
        every { issueRepositoryAdapter.findIssuesByNumistaId(coinTypeNumistaId) } returns existingIssues
        every { issueRepositoryAdapter.linkIssuesToCoin(coinId, listOf(issue1Id, issue2Id)) } returns Unit

        // When
        val result = service.fetchIssuesFromNumista(coinId, coinTypeNumistaId)

        // Then
        assertTrue(result)
        verify(exactly = 1) { issueRepositoryAdapter.coinHasIssues(coinId) }
        verify(exactly = 1) { issueRepositoryAdapter.findIssuesByNumistaId(coinTypeNumistaId) }
        verify(exactly = 0) { numistaClient.getIssues(any()) } // API NOT called!
        verify(exactly = 0) { issueRepositoryAdapter.saveAll(any()) } // Nothing saved!
        verify(exactly = 1) { issueRepositoryAdapter.linkIssuesToCoin(coinId, listOf(issue1Id, issue2Id)) }
    }

    @Test
    fun `should fetch from API when no issues exist for coin type`() {
        // Given
        val coinId = UUID.randomUUID()
        val coinTypeNumistaId = "12345"
        val issue1Id = UUID.randomUUID()
        val issue2Id = UUID.randomUUID()
        val issueResponses = listOf(
            NumistaIssueResponse(
                id = 101,
                isDated = true,
                year = 2020,
                gregorianYear = 2020,
                minYear = null,
                maxYear = null,
                mintLetter = "D",
                mintage = 1_000_000,
                comment = "Regular issue"
            ),
            NumistaIssueResponse(
                id = 102,
                isDated = true,
                year = 2021,
                gregorianYear = 2021,
                minYear = null,
                maxYear = null,
                mintLetter = "A",
                mintage = 500_000,
                comment = "Proof"
            )
        )

        every { issueRepositoryAdapter.coinHasIssues(coinId) } returns false
        every { issueRepositoryAdapter.findIssuesByNumistaId(coinTypeNumistaId) } returns emptyList()
        every { numistaClient.getIssues(coinTypeNumistaId) } returns issueResponses
        every { issueRepositoryAdapter.saveAll(any()) } returns listOf(
            Issue(id = issue1Id, numistaId = "101", year = 2020, mintage = 1_000_000, mintLetter = "D", comment = "Regular issue", isProof = false),
            Issue(id = issue2Id, numistaId = "102", year = 2021, mintage = 500_000, mintLetter = "A", comment = "Proof", isProof = true)
        )
        every { issueRepositoryAdapter.linkIssuesToCoin(coinId, listOf(issue1Id, issue2Id)) } returns Unit

        // When
        val result = service.fetchIssuesFromNumista(coinId, coinTypeNumistaId)

        // Then
        assertTrue(result)
        verify(exactly = 1) { issueRepositoryAdapter.coinHasIssues(coinId) }
        verify(exactly = 1) { issueRepositoryAdapter.findIssuesByNumistaId(coinTypeNumistaId) }
        verify(exactly = 1) { numistaClient.getIssues(coinTypeNumistaId) }
        verify(exactly = 1) { issueRepositoryAdapter.saveAll(match { it.size == 2 }) }
        verify(exactly = 1) { issueRepositoryAdapter.linkIssuesToCoin(coinId, listOf(issue1Id, issue2Id)) }
    }

    @Test
    fun `should return false when Numista returns empty list`() {
        // Given
        val coinId = UUID.randomUUID()
        val coinTypeNumistaId = "12345"
        every { issueRepositoryAdapter.coinHasIssues(coinId) } returns false
        every { issueRepositoryAdapter.findIssuesByNumistaId(coinTypeNumistaId) } returns emptyList()
        every { numistaClient.getIssues(coinTypeNumistaId) } returns emptyList()

        // When
        val result = service.fetchIssuesFromNumista(coinId, coinTypeNumistaId)

        // Then
        assertFalse(result)
        verify(exactly = 1) { issueRepositoryAdapter.coinHasIssues(coinId) }
        verify(exactly = 1) { issueRepositoryAdapter.findIssuesByNumistaId(coinTypeNumistaId) }
        verify(exactly = 1) { numistaClient.getIssues(coinTypeNumistaId) }
        verify(exactly = 0) { issueRepositoryAdapter.saveAll(any()) }
    }

    @Test
    fun `should handle API exception gracefully`() {
        // Given
        val coinId = UUID.randomUUID()
        val coinTypeNumistaId = "12345"
        every { issueRepositoryAdapter.coinHasIssues(coinId) } returns false
        every { issueRepositoryAdapter.findIssuesByNumistaId(coinTypeNumistaId) } returns emptyList()
        every { numistaClient.getIssues(coinTypeNumistaId) } throws RuntimeException("API error")

        // When/Then - exception should propagate
        try {
            service.fetchIssuesFromNumista(coinId, coinTypeNumistaId)
            assert(false) { "Should have thrown exception" }
        } catch (e: RuntimeException) {
            assert(e.message == "API error")
        }

        verify(exactly = 1) { issueRepositoryAdapter.coinHasIssues(coinId) }
        verify(exactly = 1) { issueRepositoryAdapter.findIssuesByNumistaId(coinTypeNumistaId) }
        verify(exactly = 1) { numistaClient.getIssues(coinTypeNumistaId) }
    }

    @Test
    fun `should use extension function for conversion`() {
        // Given
        val coinId = UUID.randomUUID()
        val coinTypeNumistaId = "12345"
        val issueId = UUID.randomUUID()
        val issueResponse = NumistaIssueResponse(
            id = 101,
            isDated = true,
            year = 1443,  // Islamic calendar
            gregorianYear = 2021,  // Should be preferred
            minYear = null,
            maxYear = null,
            mintLetter = "D",
            mintage = 1_000_000,
            comment = "Berlin mint"
        )

        every { issueRepositoryAdapter.coinHasIssues(coinId) } returns false
        every { issueRepositoryAdapter.findIssuesByNumistaId(coinTypeNumistaId) } returns emptyList()
        every { numistaClient.getIssues(coinTypeNumistaId) } returns listOf(issueResponse)
        every { issueRepositoryAdapter.saveAll(any()) } returns listOf(
            Issue(id = issueId, numistaId = "101", year = 2021, mintage = 1_000_000, mintLetter = "D", comment = "Berlin mint", isProof = false)
        )
        every { issueRepositoryAdapter.linkIssuesToCoin(coinId, listOf(issueId)) } returns Unit

        // When
        val result = service.fetchIssuesFromNumista(coinId, coinTypeNumistaId)

        // Then
        assertTrue(result)
        verify(exactly = 1) {
            issueRepositoryAdapter.saveAll(match { issues ->
                issues.size == 1 &&
                issues[0].numistaId == "101" &&
                issues[0].year == 2021 // gregorianYear used
            })
        }
    }
}
