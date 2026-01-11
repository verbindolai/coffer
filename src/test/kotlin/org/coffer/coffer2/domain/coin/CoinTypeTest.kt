package org.coffer.coffer2.domain.coin

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CoinTypeTest {

    @Test
    fun `fromNumistaString should map valid types case-insensitively`() {
        assertEquals(CoinType.BULLION, CoinType.fromNumistaString("bullion coins"))
        assertEquals(CoinType.COMMEMORATIVE_CIRCULATION, CoinType.fromNumistaString("CIRCULATING COMMEMORATIVE COINS"))
        assertEquals(CoinType.STANDARD_CIRCULATION, CoinType.fromNumistaString("standard circulation coins"))
    }

    @Test
    fun `fromNumistaString should return OTHER for null or unknown input`() {
        assertEquals(CoinType.OTHER, CoinType.fromNumistaString(null))
        assertEquals(CoinType.OTHER, CoinType.fromNumistaString("unknown"))
    }
}
