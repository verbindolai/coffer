package org.coffer.coffer2.repository

import org.junit.jupiter.api.Test
import java.time.ZonedDateTime
import java.util.*
import kotlin.test.assertEquals

class IssueEntityTest {

    @Test
    fun `should convert entity to domain and back without losing data`() {
        // Given - entity with all fields populated
        val originalEntity = IssueEntity(
            id = UUID.randomUUID(),
            numistaId = "NUMISTA123",
            coinId = UUID.randomUUID(),
            year = 2023,
            mintage = 50000L,
            mintLetter = "D",
            comment = "Special edition",
            isProof = true,
            createdAt = ZonedDateTime.now()
        )

        // When - convert to domain and back
        val issue = originalEntity.toIssue()
        val roundTripEntity = IssueEntity.fromIssue(issue)

        // Then - verify key fields match
        assertEquals(originalEntity.id, UUID.fromString(roundTripEntity.id.toString()))
        assertEquals(originalEntity.numistaId, roundTripEntity.numistaId)
        assertEquals(originalEntity.coinId, UUID.fromString(roundTripEntity.coinId.toString()))
        assertEquals(originalEntity.year, roundTripEntity.year)
        assertEquals(originalEntity.mintage, roundTripEntity.mintage)
        assertEquals(originalEntity.isProof, roundTripEntity.isProof)
    }
}
