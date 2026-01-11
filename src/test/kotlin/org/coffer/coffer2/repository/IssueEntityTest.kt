package org.coffer.coffer2.repository

import org.coffer.coffer2.domain.coin.Issue
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IssueEntityTest {

    @Test
    fun `toIssue should convert entity to domain model correctly`() {
        // Given
        val entityId = UUID.randomUUID()
        val coinId = UUID.randomUUID()
        val createdAt = ZonedDateTime.now()

        val entity = IssueEntity(
            id = entityId,
            numistaId = "12345",
            coinId = coinId,
            year = 2023,
            mintage = 100000L,
            mintLetter = "W",
            comment = "Test comment",
            isProof = true,
            createdAt = createdAt
        )

        // When
        val issue = entity.toIssue()

        // Then
        assertEquals(entityId.toString(), issue.id)
        assertEquals("12345", issue.numistaId)
        assertEquals(coinId.toString(), issue.coinId)
        assertEquals(2023, issue.year)
        assertEquals(100000L, issue.mintage)
        assertEquals("W", issue.mintLetter)
        assertEquals("Test comment", issue.comment)
        assertTrue(issue.isProof)
        assertEquals(createdAt, issue.createdAt)
    }

    @Test
    fun `fromIssue should convert domain model to entity correctly`() {
        // Given
        val coinId = UUID.randomUUID()
        val createdAt = ZonedDateTime.now()

        val issue = Issue(
            id = UUID.randomUUID().toString(),
            numistaId = "67890",
            coinId = coinId.toString(),
            year = 2022,
            mintage = 50000L,
            mintLetter = "D",
            comment = "Commemorative edition",
            isProof = false,
            createdAt = createdAt
        )

        // When
        val entity = IssueEntity.fromIssue(issue)

        // Then
        assertEquals(issue.id, entity.id.toString())
        assertEquals("67890", entity.numistaId)
        assertEquals(coinId, entity.coinId)
        assertEquals(2022, entity.year)
        assertEquals(50000L, entity.mintage)
        assertEquals("D", entity.mintLetter)
        assertEquals("Commemorative edition", entity.comment)
        assertFalse(entity.isProof)
        assertEquals(createdAt, entity.createdAt)
    }

    @Test
    fun `fromIssue should generate UUID when issue id is null`() {
        // Given
        val issue = Issue(
            id = null,
            numistaId = "11111",
            coinId = UUID.randomUUID().toString(),
            year = 2021,
            mintage = null,
            mintLetter = null,
            comment = null,
            isProof = false,
            createdAt = ZonedDateTime.now()
        )

        // When
        val entity = IssueEntity.fromIssue(issue)

        // Then
        assertNotNull(entity.id)
    }

    @Test
    fun `round-trip conversion should preserve all fields`() {
        // Given
        val coinId = UUID.randomUUID()
        val createdAt = ZonedDateTime.now()

        val originalIssue = Issue(
            id = UUID.randomUUID().toString(),
            numistaId = "99999",
            coinId = coinId.toString(),
            year = 2024,
            mintage = 200000L,
            mintLetter = "S",
            comment = "Special edition",
            isProof = true,
            createdAt = createdAt
        )

        // When
        val entity = IssueEntity.fromIssue(originalIssue)
        val resultIssue = entity.toIssue()

        // Then
        assertEquals(originalIssue.id, resultIssue.id)
        assertEquals(originalIssue.numistaId, resultIssue.numistaId)
        assertEquals(originalIssue.coinId, resultIssue.coinId)
        assertEquals(originalIssue.year, resultIssue.year)
        assertEquals(originalIssue.mintage, resultIssue.mintage)
        assertEquals(originalIssue.mintLetter, resultIssue.mintLetter)
        assertEquals(originalIssue.comment, resultIssue.comment)
        assertEquals(originalIssue.isProof, resultIssue.isProof)
        assertEquals(originalIssue.createdAt, resultIssue.createdAt)
    }

    @Test
    fun `toIssue should handle nullable fields correctly`() {
        // Given
        val entity = IssueEntity(
            id = UUID.randomUUID(),
            numistaId = "22222",
            coinId = UUID.randomUUID(),
            year = null,
            mintage = null,
            mintLetter = null,
            comment = null,
            isProof = false,
            createdAt = ZonedDateTime.now()
        )

        // When
        val issue = entity.toIssue()

        // Then
        assertEquals(null, issue.year)
        assertEquals(null, issue.mintage)
        assertEquals(null, issue.mintLetter)
        assertEquals(null, issue.comment)
        assertFalse(issue.isProof)
    }

    @Test
    fun `isProof should default to false when not specified`() {
        // Given
        val issue = Issue(
            id = UUID.randomUUID().toString(),
            numistaId = "33333",
            coinId = UUID.randomUUID().toString(),
            year = 2023,
            mintage = 10000L,
            mintLetter = null,
            comment = null,
            isProof = false,
            createdAt = ZonedDateTime.now()
        )

        // When
        val entity = IssueEntity.fromIssue(issue)
        val result = entity.toIssue()

        // Then
        assertFalse(result.isProof)
    }
}
