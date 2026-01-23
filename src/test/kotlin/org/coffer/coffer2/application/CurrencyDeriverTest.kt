package org.coffer.coffer2.application

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CurrencyDeriverTest {

    // ==================== Issuer Code Lookup Tests ====================

    @Test
    fun `derive returns USD for US issuer code`() {
        assertEquals("USD", CurrencyDeriver.derive("us", null))
    }

    @Test
    fun `derive returns EUR for German issuer code`() {
        assertEquals("EUR", CurrencyDeriver.derive("de", null))
    }

    @Test
    fun `derive returns GBP for GB issuer code`() {
        assertEquals("GBP", CurrencyDeriver.derive("gb", null))
    }

    @Test
    fun `derive returns CAD for CA issuer code`() {
        assertEquals("CAD", CurrencyDeriver.derive("ca", null))
    }

    @Test
    fun `derive returns AUD for AU issuer code`() {
        assertEquals("AUD", CurrencyDeriver.derive("au", null))
    }

    @Test
    fun `derive returns EUR for multiple eurozone countries`() {
        val eurozoneCountries = listOf("fr", "it", "es", "at", "be", "nl", "pt", "ie", "fi", "gr", "hr")
        for (country in eurozoneCountries) {
            assertEquals("EUR", CurrencyDeriver.derive(country, null), "Failed for country: $country")
        }
    }

    @Test
    fun `derive handles uppercase issuer code`() {
        assertEquals("USD", CurrencyDeriver.derive("US", null))
    }

    @Test
    fun `derive handles mixed case issuer code`() {
        assertEquals("GBP", CurrencyDeriver.derive("Gb", null))
    }

    // ==================== Value Text Lookup Tests ====================

    @Test
    fun `derive returns USD for value text containing dollar`() {
        assertEquals("USD", CurrencyDeriver.derive(null, "50 Dollars"))
    }

    @Test
    fun `derive returns EUR for value text containing euro`() {
        assertEquals("EUR", CurrencyDeriver.derive(null, "2 Euro"))
    }

    @Test
    fun `derive returns GBP for value text containing pound`() {
        assertEquals("GBP", CurrencyDeriver.derive(null, "1 Pound"))
    }

    @Test
    fun `derive returns GBP for value text containing pence`() {
        assertEquals("GBP", CurrencyDeriver.derive(null, "50 Pence"))
    }

    @Test
    fun `derive returns EUR for value text containing cent`() {
        assertEquals("EUR", CurrencyDeriver.derive(null, "50 Cent"))
    }

    @Test
    fun `derive returns CHF for value text containing franc`() {
        assertEquals("CHF", CurrencyDeriver.derive(null, "5 Francs"))
    }

    @Test
    fun `derive returns JPY for value text containing yen`() {
        assertEquals("JPY", CurrencyDeriver.derive(null, "100 Yen"))
    }

    // ==================== Priority Tests ====================

    @Test
    fun `derive prioritizes value text over issuer code`() {
        // Value text says dollars (USD) but issuer is Germany (EUR)
        assertEquals("USD", CurrencyDeriver.derive("de", "50 Dollars"))
    }

    @Test
    fun `derive falls back to issuer code when value text has no match`() {
        assertEquals("EUR", CurrencyDeriver.derive("de", "50 Marks"))
    }

    // ==================== Null and Unknown Input Tests ====================

    @Test
    fun `derive returns null for null inputs`() {
        assertNull(CurrencyDeriver.derive(null, null))
    }

    @Test
    fun `derive returns null for unknown issuer code`() {
        assertNull(CurrencyDeriver.derive("xx", null))
    }

    @Test
    fun `derive returns null for unknown value text`() {
        assertNull(CurrencyDeriver.derive(null, "50 Widgets"))
    }

    @Test
    fun `derive returns null when both inputs are unknown`() {
        assertNull(CurrencyDeriver.derive("xx", "50 Widgets"))
    }
}
