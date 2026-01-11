package org.coffer.coffer2.domain.exception

import java.util.UUID

sealed class CofferDomainException(message: String) : RuntimeException(message)

class CoinNotFoundException(id: UUID) : CofferDomainException("Coin not found: $id")
