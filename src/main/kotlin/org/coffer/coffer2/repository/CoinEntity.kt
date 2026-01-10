package org.coffer.coffer2.repository

import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.coffer.coffer2.domain.coin.Coin
import java.util.UUID

@Entity
@Table(name = "coins")
data class CoinEntity(

    @Id
    private val id: UUID
) {
    fun toCoin(): Coin = Coin(

    )

    companion object {
        fun fromCoin(coin: Coin): MetalQuoteEntity = MetalQuoteEntity(
            id = UUID.randomUUID()
        )
    }
}