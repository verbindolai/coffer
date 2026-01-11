package org.coffer.coffer2.domain.coin

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CoinGradeTest {

    @Test
    fun `fromNumistaGrade should map valid codes case-insensitively`() {
        assertEquals(CoinGrade.GOOD, CoinGrade.fromNumistaGrade("g"))
        assertEquals(CoinGrade.VERY_FINE, CoinGrade.fromNumistaGrade("VF"))
        assertEquals(CoinGrade.UNCIRCULATED, CoinGrade.fromNumistaGrade("unc"))
    }

    @Test
    fun `fromNumistaGrade should return null for invalid codes`() {
        assertNull(CoinGrade.fromNumistaGrade("invalid"))
        assertNull(CoinGrade.fromNumistaGrade(""))
    }

    @Test
    fun `toNumistaGrade should return correct codes and null for PROOF`() {
        assertEquals("vf", CoinGrade.VERY_FINE.toNumistaGrade())
        assertEquals("unc", CoinGrade.UNCIRCULATED.toNumistaGrade())
        assertNull(CoinGrade.PROOF.toNumistaGrade())
    }
}
