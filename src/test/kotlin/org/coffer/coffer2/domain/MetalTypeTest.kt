package org.coffer.coffer2.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class MetalTypeTest {

    @Test
    fun `toSwissquoteSymbol should return correct symbols`() {
        assertEquals("XAU", MetalType.GOLD.toSwissquoteSymbol())
        assertEquals("XAG", MetalType.SILVER.toSwissquoteSymbol())
        assertEquals("XPT", MetalType.PLATINUM.toSwissquoteSymbol())
    }

    @Test
    fun `toSwissquoteSymbol should throw exception for OTHER`() {
        assertThrows<IllegalArgumentException> {
            MetalType.OTHER.toSwissquoteSymbol()
        }
    }
}
