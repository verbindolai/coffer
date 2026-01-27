package org.coffer.coffer2.repository

import jakarta.persistence.*
import org.coffer.coffer2.domain.MetalType
import org.coffer.coffer2.domain.coin.*
import java.math.BigDecimal
import java.time.ZonedDateTime
import java.util.*

@Entity
@Table(name = "coins")
data class CoinEntity(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false)
    val title: String,

    @Column(precision = 19, scale = 4)
    val denomination: BigDecimal?,

    @Column(nullable = false, length = 3)
    val currencyCode: String,

    @Column(nullable = false)
    val yearOfMinting: Int,

    @Column(nullable = false, length = 2)
    val issuerCountryCode: String,

    @Column(length = 50)
    val mintMark: String?,

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    val grade: CoinGrade?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val type: CoinType,

    @Column(columnDefinition = "TEXT")
    val notes: String?,

    @Column(length = 50)
    val numistaId: String?,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    val shape: CoinShape = CoinShape.UNKNOWN,

    @Column(nullable = false, precision = 19, scale = 4)
    val weightInGrams: BigDecimal,

    @Column(precision = 19, scale = 4)
    val purity: BigDecimal?,

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    val metalType: MetalType?,

    @Column
    val rarityScore: Int?,

    @Column(nullable = false)
    val createdAt: ZonedDateTime = ZonedDateTime.now(),

    @Column(precision = 19, scale = 4)
    val diameterInMillimeters: BigDecimal?,

    @Column(precision = 19, scale = 4)
    val thicknessInMillimeters: BigDecimal?,

    @Column(nullable = false)
    val quantity: Int = 1,

    @Column
    val deletedAt: ZonedDateTime? = null
) {
    fun toCoin(): Coin = Coin(
        id = id,
        title = title,
        denomination = denomination,
        currency = Currency.getInstance(currencyCode),
        yearOfMinting = YearOfMinting(yearOfMinting),
        issuerCountry = Locale.of("", issuerCountryCode),
        mintMark = mintMark?.let { MintMark(it) },
        grade = grade,
        type = type,
        notes = notes,
        numistaId = numistaId,
        shape = shape,
        weightInGrams = weightInGrams,
        purity = purity,
        metalType = metalType,
        rarity = rarityScore?.let { Rarity(it) },
        createdAt = createdAt,
        diameterInMillimeters = diameterInMillimeters,
        thicknessInMillimeters = thicknessInMillimeters,
        quantity = quantity,
        deletedAt = deletedAt
    )

    companion object {
        fun fromCoin(coin: Coin): CoinEntity = CoinEntity(
            id = coin.id,
            title = coin.title,
            denomination = coin.denomination,
            currencyCode = coin.currency.currencyCode,
            yearOfMinting = coin.yearOfMinting.year,
            issuerCountryCode = coin.issuerCountry.country,
            mintMark = coin.mintMark?.value,
            grade = coin.grade,
            type = coin.type,
            notes = coin.notes,
            numistaId = coin.numistaId,
            shape = coin.shape,
            weightInGrams = coin.weightInGrams,
            purity = coin.purity,
            metalType = coin.metalType,
            rarityScore = coin.rarity?.score,
            createdAt = coin.createdAt,
            diameterInMillimeters = coin.diameterInMillimeters,
            thicknessInMillimeters = coin.thicknessInMillimeters,
            quantity = coin.quantity,
            deletedAt = coin.deletedAt
        )
    }
}