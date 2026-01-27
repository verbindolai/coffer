package org.coffer.coffer2.repository

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.UUID

@Entity
@Table(name = "portfolio_snapshots")
data class PortfolioSnapshotEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val snapshotDate: LocalDate,

    @Column(nullable = false, length = 3)
    val currencyCode: String,

    @Column(nullable = false)
    val totalCoins: Int,

    @Column(nullable = false)
    val totalQuantity: Int,

    @Column(precision = 19, scale = 4)
    val metalValue: BigDecimal?,

    @Column(precision = 19, scale = 6)
    val goldGrams: BigDecimal?,

    @Column(precision = 19, scale = 6)
    val silverGrams: BigDecimal?,

    @Column(precision = 19, scale = 6)
    val platinumGrams: BigDecimal?,

    @Column(precision = 19, scale = 4)
    val collectorValueMin: BigDecimal?,

    @Column(precision = 19, scale = 4)
    val collectorValueMax: BigDecimal?,

    @Column(nullable = false)
    val createdAt: ZonedDateTime = ZonedDateTime.now()
)
