package org.coffer.coffer2.repository

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "metal_quotes")
data class MetalQuoteEntity(
    @Id
    private val id: UUID


)