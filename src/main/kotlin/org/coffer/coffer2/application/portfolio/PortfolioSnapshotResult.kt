package org.coffer.coffer2.application.portfolio

import org.coffer.coffer2.repository.PortfolioSnapshotEntity
import java.math.BigDecimal
import java.time.LocalDate

data class PortfolioSnapshotResult(
    val snapshotDate: LocalDate,
    val currency: String,
    val totalCoins: Int,
    val totalQuantity: Int,
    val metalValue: BigDecimal?,
    val goldGrams: BigDecimal?,
    val silverGrams: BigDecimal?,
    val platinumGrams: BigDecimal?,
    val collectorValueExact: BigDecimal?,
    val collectorValueMin: BigDecimal?,
    val collectorValueMax: BigDecimal?
) {
    companion object {
        fun from(entity: PortfolioSnapshotEntity): PortfolioSnapshotResult =
            PortfolioSnapshotResult(
                snapshotDate = entity.snapshotDate,
                currency = entity.currencyCode,
                totalCoins = entity.totalCoins,
                totalQuantity = entity.totalQuantity,
                metalValue = entity.metalValue,
                goldGrams = entity.goldGrams,
                silverGrams = entity.silverGrams,
                platinumGrams = entity.platinumGrams,
                collectorValueExact = entity.collectorValueExact,
                collectorValueMin = entity.collectorValueMin,
                collectorValueMax = entity.collectorValueMax
            )
    }
}
