package org.coffer.coffer2.repository

import org.coffer.coffer2.api.CoinSearchQuery
import org.coffer.coffer2.application.CoinRepositoryAdapter
import org.coffer.coffer2.domain.coin.Coin
import org.coffer.coffer2.domain.exception.CoinNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.domain.Specification
import org.springframework.stereotype.Component
import java.time.ZonedDateTime
import java.util.UUID

@Component
class CoinRepositoryAdapterImpl(
    private val coinRepository: CoinRepository
): CoinRepositoryAdapter {
    override fun save(coin: Coin): Coin {
        return coinRepository.save(CoinEntity.fromCoin(coin)).toCoin()
    }

    override fun findById(id: UUID): Coin? {
        return coinRepository.findByIdAndDeletedAtIsNull(id)?.toCoin()
    }

    override fun deleteById(id: UUID) {
        coinRepository.deleteById(id)
    }

    override fun existsById(id: UUID): Boolean {
        return coinRepository.existsByIdAndDeletedAtIsNull(id)
    }

    override fun search(query: CoinSearchQuery, pageable: Pageable): Page<Coin> {
        val spec = buildSpecification(query)
        return coinRepository.findAll(spec, pageable).map { it.toCoin() }
    }

    override fun findByNumistaIdIsNotNull(): List<Coin> {
        return coinRepository.findByNumistaIdIsNotNullAndDeletedAtIsNull().map { it.toCoin() }
    }

    override fun findAll(): List<Coin> {
        return coinRepository.findByDeletedAtIsNull().map { it.toCoin() }
    }

    override fun softDelete(id: UUID): Coin {
        val entity = coinRepository.findByIdAndDeletedAtIsNull(id)
            ?: throw CoinNotFoundException(id)
        val updated = entity.copy(deletedAt = ZonedDateTime.now())
        return coinRepository.save(updated).toCoin()
    }

    override fun findAllIncludingDeletedAfter(cutoff: ZonedDateTime): List<Coin> {
        return coinRepository.findByDeletedAtIsNullOrDeletedAtAfter(cutoff).map { it.toCoin() }
    }

    override fun existsByNumistaId(numistaId: String): Boolean {
        return coinRepository.existsByNumistaIdAndDeletedAtIsNull(numistaId)
    }

    private fun buildSpecification(query: CoinSearchQuery): Specification<CoinEntity> {
        val specs = mutableListOf<Specification<CoinEntity>>()

        specs.add(deletedAtIsNull())
        query.country?.let { specs.add(countryEquals(it)) }
        query.denomination?.let { specs.add(denominationEquals(it)) }
        query.grade?.let { specs.add(gradeEquals(it)) }
        query.coinType?.let { specs.add(typeEquals(it)) }
        query.yearFrom?.let { specs.add(yearGreaterThanOrEqual(it)) }
        query.yearTo?.let { specs.add(yearLessThanOrEqual(it)) }
        query.metalType?.let { specs.add(metalTypeEquals(it)) }
        query.shape?.let { specs.add(shapeEquals(it)) }
        query.currency?.let { specs.add(currencyEquals(it)) }
        query.title?.let { specs.add(titleContains(it)) }
        query.numistaId?.let { specs.add(numistaIdEquals(it)) }

        return specs.reduceOrNull { acc, spec -> acc.and(spec) }
            ?: Specification { _, _, cb -> cb.conjunction() }
    }

    private fun deletedAtIsNull(): Specification<CoinEntity> =
        Specification { root, _, cb -> cb.isNull(root.get<ZonedDateTime>("deletedAt")) }

    private fun countryEquals(country: String): Specification<CoinEntity> =
        Specification { root, _, cb -> cb.equal(root.get<String>("issuerCountryCode"), country.uppercase()) }

    private fun denominationEquals(denomination: String): Specification<CoinEntity> =
        Specification { root, _, cb -> cb.equal(root.get<String>("denomination"), denomination.toBigDecimalOrNull()) }

    private fun gradeEquals(grade: org.coffer.coffer2.domain.coin.CoinGrade): Specification<CoinEntity> =
        Specification { root, _, cb -> cb.equal(root.get<String>("grade"), grade) }

    private fun typeEquals(type: org.coffer.coffer2.domain.coin.CoinType): Specification<CoinEntity> =
        Specification { root, _, cb -> cb.equal(root.get<String>("type"), type) }

    private fun yearGreaterThanOrEqual(year: Int): Specification<CoinEntity> =
        Specification { root, _, cb -> cb.greaterThanOrEqualTo(root.get("yearOfMinting"), year) }

    private fun yearLessThanOrEqual(year: Int): Specification<CoinEntity> =
        Specification { root, _, cb -> cb.lessThanOrEqualTo(root.get("yearOfMinting"), year) }

    private fun metalTypeEquals(metalType: org.coffer.coffer2.domain.MetalType): Specification<CoinEntity> =
        Specification { root, _, cb -> cb.equal(root.get<String>("metalType"), metalType) }

    private fun shapeEquals(shape: org.coffer.coffer2.domain.coin.CoinShape): Specification<CoinEntity> =
        Specification { root, _, cb -> cb.equal(root.get<String>("shape"), shape) }

    private fun currencyEquals(currency: String): Specification<CoinEntity> =
        Specification { root, _, cb -> cb.equal(root.get<String>("currencyCode"), currency.uppercase()) }

    private fun titleContains(title: String): Specification<CoinEntity> =
        Specification { root, _, cb -> cb.like(cb.lower(root.get("title")), "%${title.lowercase()}%") }

    private fun numistaIdEquals(numistaId: String): Specification<CoinEntity> =
        Specification { root, _, cb -> cb.equal(root.get<String>("numistaId"), numistaId) }
}