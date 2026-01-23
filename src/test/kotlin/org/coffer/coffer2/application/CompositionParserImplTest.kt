package org.coffer.coffer2.application

import org.coffer.coffer2.domain.MetalType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CompositionParserImplTest {

    private val parser = CompositionParserImpl()

    // ==================== Null/Blank Input Tests ====================

    @Test
    fun `parse returns null metal and purity for null input`() {
        val result = parser.parse(null)
        assertNull(result.metalType)
        assertNull(result.purity)
    }

    @Test
    fun `parse returns null metal and purity for blank input`() {
        val result = parser.parse("   ")
        assertNull(result.metalType)
        assertNull(result.purity)
    }

    @Test
    fun `parse returns null metal and purity for empty string`() {
        val result = parser.parse("")
        assertNull(result.metalType)
        assertNull(result.purity)
    }

    // ==================== Precious Metal Detection Tests ====================

    @Test
    fun `parse detects gold`() {
        val result = parser.parse("Gold (.9999)")
        assertEquals(MetalType.GOLD, result.metalType)
    }

    @Test
    fun `parse detects silver`() {
        val result = parser.parse("Silver 999")
        assertEquals(MetalType.SILVER, result.metalType)
    }

    @Test
    fun `parse detects platinum`() {
        val result = parser.parse("Platinum 999")
        assertEquals(MetalType.PLATINUM, result.metalType)
    }

    @Test
    fun `parse detects gold case insensitively`() {
        val result = parser.parse("GOLD 999")
        assertEquals(MetalType.GOLD, result.metalType)
    }

    // ==================== Purity Pattern Tests ====================

    @Test
    fun `parse extracts purity from parenthetical decimal pattern`() {
        val result = parser.parse("Gold (.9999)")
        assertEquals(999, result.purity)
    }

    @Test
    fun `parse extracts purity from three digit parenthetical`() {
        val result = parser.parse("Gold (.999)")
        assertEquals(999, result.purity)
    }

    @Test
    fun `parse extracts purity from permille pattern`() {
        val result = parser.parse("Gold 900\u2030")
        assertEquals(900, result.purity)
    }

    @Test
    fun `parse extracts purity from plain number after metal`() {
        val result = parser.parse("Gold 916.7")
        assertEquals(916, result.purity)
    }

    @Test
    fun `parse extracts purity from plain three digit number`() {
        val result = parser.parse("Silver 925")
        assertEquals(925, result.purity)
    }

    @Test
    fun `parse extracts purity from decimal without parens`() {
        val result = parser.parse("0.999 Gold")
        assertEquals(999, result.purity)
    }

    @Test
    fun `parse extracts purity from 0_9999 format`() {
        val result = parser.parse("0.9999 Gold")
        assertEquals(999, result.purity)
    }

    // ==================== Exclusion Keyword Tests ====================

    @Test
    fun `parse returns null metal type for plated coins`() {
        val result = parser.parse("Gold plated copper")
        assertNull(result.metalType)
    }

    @Test
    fun `parse returns null metal type for bi-metallic coins`() {
        val result = parser.parse("Bi-metallic Gold and Silver")
        assertNull(result.metalType)
    }

    @Test
    fun `parse returns null metal type for bimetallic coins`() {
        val result = parser.parse("Bimetallic Gold and Silver")
        assertNull(result.metalType)
    }

    @Test
    fun `parse returns null metal type for clad coins`() {
        val result = parser.parse("Silver clad copper")
        assertNull(result.metalType)
    }

    // ==================== Base Metal Tests ====================

    @Test
    fun `parse detects copper as base metal`() {
        val result = parser.parse("Copper")
        assertEquals(MetalType.BASE_METAL, result.metalType)
    }

    @Test
    fun `parse detects cupronickel as base metal`() {
        val result = parser.parse("Cupronickel")
        assertEquals(MetalType.BASE_METAL, result.metalType)
    }

    @Test
    fun `parse detects brass as base metal`() {
        val result = parser.parse("Brass")
        assertEquals(MetalType.BASE_METAL, result.metalType)
    }

    @Test
    fun `parse detects bronze as base metal`() {
        val result = parser.parse("Bronze")
        assertEquals(MetalType.BASE_METAL, result.metalType)
    }

    @Test
    fun `parse detects steel as base metal`() {
        val result = parser.parse("Steel")
        assertEquals(MetalType.BASE_METAL, result.metalType)
    }

    @Test
    fun `parse detects nickel in base metals list as base metal`() {
        val result = parser.parse("Nickel")
        assertEquals(MetalType.BASE_METAL, result.metalType)
    }

    // ==================== Unknown Text Tests ====================

    @Test
    fun `parse returns null for unknown composition text`() {
        val result = parser.parse("Unknown material")
        assertNull(result.metalType)
        assertNull(result.purity)
    }

    @Test
    fun `parse returns null for text with no metal keywords`() {
        val result = parser.parse("Some random text 999")
        assertNull(result.metalType)
    }
}
