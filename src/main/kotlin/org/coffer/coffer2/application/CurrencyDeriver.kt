package org.coffer.coffer2.application

object CurrencyDeriver {

    private val COUNTRY_TO_CURRENCY = mapOf(
        "us" to "USD",
        "ca" to "CAD",
        "au" to "AUD",
        "gb" to "GBP",
        "cn" to "CNY",
        "jp" to "JPY",
        "ch" to "CHF",
        "mx" to "MXN",
        "za" to "ZAR",
        "de" to "EUR", "fr" to "EUR", "it" to "EUR", "es" to "EUR",
        "at" to "EUR", "be" to "EUR", "nl" to "EUR", "pt" to "EUR",
        "ie" to "EUR", "fi" to "EUR", "gr" to "EUR", "lu" to "EUR",
        "sk" to "EUR", "si" to "EUR", "ee" to "EUR", "lv" to "EUR",
        "lt" to "EUR", "cy" to "EUR", "mt" to "EUR", "hr" to "EUR"
    )

    private val VALUE_TEXT_CURRENCIES = mapOf(
        "dollar" to "USD",
        "euro" to "EUR",
        "pound" to "GBP",
        "franc" to "CHF",
        "yen" to "JPY",
        "yuan" to "CNY",
        "rand" to "ZAR",
        "peso" to "MXN",
        "krona" to "SEK",
        "krone" to "NOK",
        "pence" to "GBP",
        "cent" to "EUR"
    )

    fun derive(issuerCode: String?, valueText: String?): String? {
        val textLower = valueText?.lowercase()
        if (textLower != null) {
            VALUE_TEXT_CURRENCIES.entries.firstOrNull { (keyword, _) ->
                textLower.contains(keyword)
            }?.let { return it.value }
        }
        return issuerCode?.lowercase()?.let { COUNTRY_TO_CURRENCY[it] }
    }
}
