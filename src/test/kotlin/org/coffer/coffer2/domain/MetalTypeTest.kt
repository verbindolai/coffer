package org.coffer.coffer2.domain

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class MetalTypeTest {

    @Test
    fun `toSymbol should return correct symbols`() {
        assertEquals("XAU", MetalType.GOLD.toSymbol())
        assertEquals("XAG", MetalType.SILVER.toSymbol())
        assertEquals("XPT", MetalType.PLATINUM.toSymbol())
    }

    @Test
    fun `toSymbol should throw exception for OTHER`() {
        assertThrows<IllegalArgumentException> {
            MetalType.OTHER.toSymbol()
        }
    }
}
